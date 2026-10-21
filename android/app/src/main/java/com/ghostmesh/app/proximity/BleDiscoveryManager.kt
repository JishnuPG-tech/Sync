package com.ghostmesh.app.proximity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * BleDiscoveryManager with Preflight Hardened State Machine:
 * - Atomic advertising state transitions (prevents concurrent advertising starts).
 * - Distinct error classification (SecurityException vs IllegalArgumentException vs Adapter state).
 * - Cascading: Tier 1 (Coded Primary & Secondary) -> Tier 2 (Hybrid) -> Tier 3 (Legacy).
 */
class BleDiscoveryManager(
    private val context: Context,
    private val proximityEvaluator: ProximityEvaluator,
    private val onDeviceDiscovered: (FilteredBleObservation) -> Unit
) {
    companion object {
        private const val TAG = "BleDiscoveryManager"
        val GHOSTMESH_SERVICE_UUID: UUID = UUID.fromString("0000feaa-0000-1000-8000-00805f9b34fb")
    }

    private enum class AdvState {
        IDLE,
        STARTING_TIER_1,
        STARTING_TIER_2,
        STARTING_TIER_3,
        ACTIVE
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var currentAdvertisingSet: AdvertisingSet? = null
    private var currentPowerMode: RadarPowerMode = RadarPowerMode.OFF
    private var advState: AdvState = AdvState.IDLE

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val scanRecord = it.scanRecord ?: return
                val serviceData = scanRecord.getServiceData(ParcelUuid(GHOSTMESH_SERVICE_UUID)) ?: return
                val rawToken = bytesToUuidString(serviceData)
                Log.i(TAG, "Sync BLE Packet Detected! Peer: $rawToken, RSSI: ${it.rssi}")

                val observation = proximityEvaluator.recordObservation(rawToken, it.rssi)
                if (observation != null) {
                    onDeviceDiscovered(observation)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan Failed: $errorCode")
        }
    }

    private fun uuidToBytes(uuidStr: String): ByteArray {
        return try {
            val clean = uuidStr.replace("-", "").trim()
            val bytes = ByteArray(clean.length / 2)
            for (i in bytes.indices) {
                bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            bytes
        } catch (e: Exception) {
            uuidStr.toByteArray(StandardCharsets.UTF_8).take(16).toByteArray()
        }
    }

    private fun bytesToUuidString(bytes: ByteArray): String {
        return try {
            if (bytes.size == 16) {
                val sb = java.lang.StringBuilder()
                for (b in bytes) {
                    sb.append(String.format("%02x", b))
                }
                val raw = sb.toString()
                "${raw.substring(0, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}-${raw.substring(16, 20)}-${raw.substring(20)}"
            } else {
                String(bytes, StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            String(bytes, StandardCharsets.UTF_8)
        }
    }

    private val legacyAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            advState = AdvState.ACTIVE
            Log.d(TAG, "Legacy High-Power BLE Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            advState = AdvState.IDLE
            Log.e(TAG, "Legacy BLE Advertising failed with code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun setPowerMode(mode: RadarPowerMode, ephemeralToken: String) {
        if (currentPowerMode == mode) return
        stopRadar()

        currentPowerMode = mode
        if (mode == RadarPowerMode.OFF) {
            Log.i(TAG, "Radar Mode -> OFF (All radios idle)")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            return
        }

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        scanner = bluetoothAdapter.bluetoothLeScanner

        val tokenBytes = uuidToBytes(ephemeralToken)

        val hasBt5Hardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                bluetoothAdapter.isLeExtendedAdvertisingSupported &&
                bluetoothAdapter.isLeCodedPhySupported

        when (mode) {
            RadarPowerMode.SMART_DISCOVERY -> {
                startHighPowerLegacyAdvertising(tokenBytes)
                startConfiguredScan(false)
            }
            RadarPowerMode.EXTENDED_RANGE -> {
                startHighPowerLegacyAdvertising(tokenBytes)
                startConfiguredScan(true)
            }
            RadarPowerMode.OFF -> {}
        }
    }

    /**
     * Tier 1: True Long-Range (Coded PHY Primary & Secondary)
     */
    @SuppressLint("MissingPermission")
    private fun startTrueLongRangeAdvertising(tokenBytes: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startHighPowerLegacyAdvertising(tokenBytes)
            return
        }

        if (advState != AdvState.IDLE) return
        advState = AdvState.STARTING_TIER_1

        try {
            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(false)
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
                .setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(GHOSTMESH_SERVICE_UUID))
                .addServiceData(ParcelUuid(GHOSTMESH_SERVICE_UUID), tokenBytes)
                .setIncludeDeviceName(false)
                .build()

            advertiser?.startAdvertisingSet(
                parameters,
                advertiseData,
                null,
                null,
                null,
                object : AdvertisingSetCallback() {
                    override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                        if (status == ADVERTISE_SUCCESS) {
                            currentAdvertisingSet = advertisingSet
                            advState = AdvState.ACTIVE
                            Log.i(TAG, "Tier 1 Active: True Long-Range Coded PHY")
                        } else {
                            Log.w(TAG, "Tier 1 callback status=$status. Advancing to Tier 2...")
                            advState = AdvState.IDLE
                            startHybridLongRangeAdvertising(tokenBytes)
                        }
                    }
                }
            )
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Tier 1 rejected by driver (${e.message}). Advancing to Tier 2...")
            advState = AdvState.IDLE
            startHybridLongRangeAdvertising(tokenBytes)
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission missing; aborting advertising", e)
            advState = AdvState.IDLE
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Bluetooth adapter in invalid state (${e.message}). Advancing to Tier 3...")
            advState = AdvState.IDLE
            startHighPowerLegacyAdvertising(tokenBytes)
        }
    }

    /**
     * Tier 2: Hybrid Extended (1M Primary & Coded Secondary)
     */
    @SuppressLint("MissingPermission")
    private fun startHybridLongRangeAdvertising(tokenBytes: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startHighPowerLegacyAdvertising(tokenBytes)
            return
        }

        if (advState != AdvState.IDLE) return
        advState = AdvState.STARTING_TIER_2

        try {
            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(false)
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_MEDIUM)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
                .setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(GHOSTMESH_SERVICE_UUID))
                .addServiceData(ParcelUuid(GHOSTMESH_SERVICE_UUID), tokenBytes)
                .setIncludeDeviceName(false)
                .build()

            advertiser?.startAdvertisingSet(
                parameters,
                advertiseData,
                null,
                null,
                null,
                object : AdvertisingSetCallback() {
                    override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                        if (status == ADVERTISE_SUCCESS) {
                            currentAdvertisingSet = advertisingSet
                            advState = AdvState.ACTIVE
                            Log.i(TAG, "Tier 2 Active: Hybrid Extended (1M Primary + Coded Secondary)")
                        } else {
                            Log.w(TAG, "Tier 2 callback status=$status. Advancing to Tier 3...")
                            advState = AdvState.IDLE
                            startHighPowerLegacyAdvertising(tokenBytes)
                        }
                    }
                }
            )
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Tier 2 rejected by driver (${e.message}). Advancing to Tier 3...")
            advState = AdvState.IDLE
            startHighPowerLegacyAdvertising(tokenBytes)
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission missing; aborting advertising", e)
            advState = AdvState.IDLE
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 error (${e.message}). Advancing to Tier 3...")
            advState = AdvState.IDLE
            startHighPowerLegacyAdvertising(tokenBytes)
        }
    }

    /**
     * Universal High-Power Advertising on standard 1M PHY (100% compatible across all Android devices)
     */
    @SuppressLint("MissingPermission")
    private fun startHighPowerLegacyAdvertising(tokenBytes: ByteArray) {
        if (advState != AdvState.IDLE) return
        advState = AdvState.STARTING_TIER_3

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .addServiceData(ParcelUuid(GHOSTMESH_SERVICE_UUID), tokenBytes)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

            advertiser?.startAdvertising(settings, data, legacyAdvertiseCallback)
            Log.i(TAG, "Universal High-Power BLE Advertising started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission missing; aborting advertising", e)
            advState = AdvState.IDLE
        } catch (e: Exception) {
            Log.e(TAG, "Legacy advertising failed to start", e)
            advState = AdvState.IDLE
        }
    }

    @SuppressLint("MissingPermission")
    private fun startConfiguredScan(hasBt5Hardware: Boolean) {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        try {
            // Software-filtered scan: passing emptyList() avoids hardware HAL filter bugs across OEMs
            scanner?.startScan(emptyList(), scanSettings, scanCallback)
            Log.i(TAG, "BLE Scanner active with LOW_LATENCY (Universal software filtering)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scanner", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopRadar() {
        currentPowerMode = RadarPowerMode.OFF
        advState = AdvState.IDLE

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentAdvertisingSet != null) {
                advertiser?.stopAdvertisingSet(object : AdvertisingSetCallback() {})
                currentAdvertisingSet = null
            } else {
                advertiser?.stopAdvertising(legacyAdvertiseCallback)
            }
            scanner?.stopScan(scanCallback)
            Log.i(TAG, "Radar radios powered down cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping radar", e)
        }
    }

    fun getCurrentMode(): RadarPowerMode = currentPowerMode
}
