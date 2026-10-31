package com.spott.app.proximity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Observable Discovery State for the UI & Lifecycle Controllers
 */
sealed class DiscoveryState {
    object Idle : DiscoveryState()
    object BluetoothDisabled : DiscoveryState()
    object MissingPermissions : DiscoveryState()
    object AdvertisingUnsupported : DiscoveryState()
    data class Active(
        val isAdvertising: Boolean,
        val isScanning: Boolean,
        val isExtendedPhy: Boolean
    ) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

/**
 * Sync BLE Discovery Manager
 * Architecture & Spec:
 * - Dedicated Sync Service UUID: 7b115359-9942-4f38-958b-0e54911d3312
 * - Compact 9-Byte Payload: [0x01 Protocol Version] + [8-Byte Ephemeral Token]
 *   Total legacy AD size: 3 bytes (flags) + 27 bytes (service data) = 30 bytes <= 31 byte limit!
 * - Dual Paths:
 *     Path A: Standard BLE (1M PHY, Universal 100% device compatibility)
 *     Path B: LE Coded PHY (True Long-Range on supported Android 8.0+ hardware)
 * - Retains matching AdvertisingSet callbacks for clean start/stop lifecycles.
 * - Hardware ScanFilter with version mask to prevent screen-off scan drops.
 * - Auto-recovery via ACTION_STATE_CHANGED broadcast receiver.
 */
class BleDiscoveryManager(
    private val context: Context,
    private val proximityEvaluator: ProximityEvaluator,
    private val onDeviceDiscovered: (FilteredBleObservation) -> Unit
) {
    companion object {
        private const val TAG = "BleDiscoveryManager"
        const val PROTOCOL_VERSION: Byte = 0x01

        // Dedicated 128-bit Service UUID for Sync (0x5359 in 1st chunk represents ASCII 'S' & 'Y')
        val SYNC_SERVICE_UUID: UUID = UUID.fromString("7b115359-9942-4f38-958b-0e54911d3312")
        val SYNC_SERVICE_PARCEL: ParcelUuid = ParcelUuid(SYNC_SERVICE_UUID)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private var currentAdvertisingSet: AdvertisingSet? = null
    private var targetPowerMode: RadarPowerMode = RadarPowerMode.OFF
    private var currentTokenHex: String = ""
    private var currentDisplayName: String = ""

    private var isAdvertising: Boolean = false
    private var isScanning: Boolean = false
    private var isExtendedActive: Boolean = false
    private var isGhostMode: Boolean = false

    fun setGhostMode(enabled: Boolean) {
        if (isGhostMode == enabled) return
        isGhostMode = enabled
        android.util.Log.i(TAG, "Ghost Mode updated -> " + isGhostMode)
        if (isGhostMode) {
            stopAdvertisingOnly()
        } else if (targetPowerMode != RadarPowerMode.OFF && !isAdvertising) {
            val adapter = bluetoothAdapter
            val hasBt5Hardware = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                    adapter?.isLeExtendedAdvertisingSupported == true &&
                    adapter?.isLeCodedPhySupported == true
            if (targetPowerMode == RadarPowerMode.EXTENDED_RANGE && hasBt5Hardware) {
                startExtendedCodedAdvertising()
            } else {
                startStandardAdvertising()
            }
        }
    }

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private var receiverRegistered: Boolean = false
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.i(TAG, "Bluetooth turned ON. Auto-starting Sync radios if radar is active...")
                        if (targetPowerMode != RadarPowerMode.OFF && currentTokenHex.isNotBlank()) {
                            startRadios()
                        }
                    }
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        Log.i(TAG, "Bluetooth turned OFF. Cleaning radio states...")
                        isAdvertising = false
                        isScanning = false
                        _discoveryState.value = DiscoveryState.BluetoothDisabled
                    }
                }
            }
        }
    }

    init {
        try {
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(bluetoothReceiver, filter)
            receiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register bluetooth broadcast receiver", e)
        }
    }

    /**
     * Scanner Callback with Protocol Verification & Parsing
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            isScanning = true
            result?.let {
                val scanRecord = it.scanRecord ?: return
                var serviceData = scanRecord.getServiceData(SYNC_SERVICE_PARCEL)
                
                // Fallback: check all service data entries if parcel mapping had strict uuid format differences
                if (serviceData == null) {
                    for ((parcel, bytes) in scanRecord.serviceData) {
                        if (parcel.uuid == SYNC_SERVICE_UUID || parcel.uuid.toString().equals(SYNC_SERVICE_UUID.toString(), ignoreCase = true)) {
                            serviceData = bytes
                            break
                        }
                    }
                }

                if (serviceData == null) return

                // Validate minimum length (1 byte version + 8 bytes token = 9 bytes)
                if (serviceData.size < 9) return
                if (serviceData[0] != PROTOCOL_VERSION) return

                val tokenBytes = serviceData.sliceArray(1..8)
                val rawTokenHex = bytesToHex(tokenBytes)
                
                // Extract instant broadcast name from scanRecord (device name or scan response)
                val broadcastName = scanRecord.deviceName?.takeIf { it.isNotBlank() }
                Log.i(TAG, "Sync BLE Packet Captured! Token: $rawTokenHex, Name: $broadcastName, RSSI: ${it.rssi}")

                val observation = proximityEvaluator.recordObservation(rawTokenHex, it.rssi, broadcastName)
                if (observation != null) {
                    onDeviceDiscovered(observation)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                isScanning = true
            } else {
                isScanning = false
                Log.e(TAG, "BLE Scan Failed: $errorCode")
                _discoveryState.value = DiscoveryState.Error("Scan failed with code: $errorCode")
            }
        }
    }

    /**
     * Legacy (1M PHY) Advertiser Callback
     */
    private val legacyAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            isExtendedActive = false
            Log.i(TAG, "Standard BLE Advertising (1M PHY) started successfully")
            updateActiveState()
        }

        override fun onStartFailure(errorCode: Int) {
            if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                isAdvertising = true
                updateActiveState()
            } else {
                isAdvertising = false
                Log.e(TAG, "Standard BLE Advertising failed with code: $errorCode")
                _discoveryState.value = DiscoveryState.Error("Advertising failed with code: $errorCode")
            }
        }
    }

    /**
     * Extended (LE Coded PHY) Advertiser Callback
     */
    private val extendedAdvertiseCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                currentAdvertisingSet = advertisingSet
                isAdvertising = true
                isExtendedActive = true
                Log.i(TAG, "LE Coded PHY Extended Advertising started successfully")
                updateActiveState()
            } else {
                Log.w(TAG, "LE Coded PHY failed (status=$status). Falling back to Standard 1M PHY...")
                isExtendedActive = false
                startStandardAdvertising()
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            currentAdvertisingSet = null
            isAdvertising = false
            isExtendedActive = false
        }
    }

    @SuppressLint("MissingPermission")
    fun setPowerMode(mode: RadarPowerMode, token: String, displayName: String = "") {
        val modeChanged = targetPowerMode != mode
        val tokenChanged = currentTokenHex != token && token.isNotBlank()
        val nameChanged = currentDisplayName != displayName && displayName.isNotBlank()

        targetPowerMode = mode
        if (token.isNotBlank()) {
            // Keep normalized 16-hex characters (8 bytes)
            currentTokenHex = token.replace("-", "").take(16).lowercase()
        }
        if (displayName.isNotBlank()) {
            currentDisplayName = displayName.take(20)
        }

        if (mode == RadarPowerMode.OFF) {
            Log.i(TAG, "Radar Mode -> OFF (All radios powering down)")
            stopRadar()
            return
        }

        if (modeChanged || tokenChanged || nameChanged || !isAdvertising || !isScanning) {
            startRadios()
        }
    }

    @SuppressLint("MissingPermission")
    fun startRadios() {
        if (targetPowerMode == RadarPowerMode.OFF) return

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _discoveryState.value = DiscoveryState.BluetoothDisabled
            Log.w(TAG, "Bluetooth not available or disabled")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScan = context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val hasAdv = context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            if (!hasScan || !hasAdv) {
                _discoveryState.value = DiscoveryState.MissingPermissions
                Log.w(TAG, "Bluetooth permissions missing")
                return
            }
        }

        advertiser = adapter.bluetoothLeAdvertiser
        scanner = adapter.bluetoothLeScanner

        if (advertiser == null) {
            _discoveryState.value = DiscoveryState.AdvertisingUnsupported
            Log.w(TAG, "BluetoothLeAdvertiser not supported by chipset")
            return
        }

        if (currentTokenHex.isBlank()) {
            Log.w(TAG, "No token available yet for advertising")
            return
        }

        val hasBt5Hardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                adapter.isLeExtendedAdvertisingSupported &&
                adapter.isLeCodedPhySupported

        // 1. Start Advertising Path (ONLY if Ghost Mode is disabled!)
        if (!isAdvertising && !isGhostMode) {
            if (targetPowerMode == RadarPowerMode.EXTENDED_RANGE && hasBt5Hardware) {
                startExtendedCodedAdvertising()
            } else {
                startStandardAdvertising()
            }
        } else if (isGhostMode && isAdvertising) {
            stopAdvertisingOnly()
        }

        // 2. Start Scanning Path
        if (!isScanning) {
            val useExtendedScan = targetPowerMode == RadarPowerMode.EXTENDED_RANGE && hasBt5Hardware
            startConfiguredScan(useExtendedScan)
        }
    }

    /**
     * Path A: Standard BLE (Universal 1M PHY)
     */
    @SuppressLint("MissingPermission")
    private fun startStandardAdvertising() {
        val payloadBytes = buildSyncPacket()
        val advMode = if (targetPowerMode == RadarPowerMode.SMART_DISCOVERY) {
            AdvertiseSettings.ADVERTISE_MODE_BALANCED
        } else {
            AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
        }
        val txPower = if (targetPowerMode == RadarPowerMode.SMART_DISCOVERY) {
            AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
        } else {
            AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
        }

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(advMode)
                .setTxPowerLevel(txPower)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .addServiceData(SYNC_SERVICE_PARCEL, payloadBytes)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            // Set local Bluetooth adapter name to current user's display name if permitted
            try {
                if (currentDisplayName.isNotBlank() && bluetoothAdapter?.name != currentDisplayName) {
                    bluetoothAdapter?.name = currentDisplayName
                }
            } catch (_: Exception) {}

            advertiser?.startAdvertising(settings, data, scanResponse, legacyAdvertiseCallback)
            Log.i(TAG, "Launching Standard BLE Advertising with Scan Response (Name: $currentDisplayName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Standard BLE Advertising", e)
            isAdvertising = false
            _discoveryState.value = DiscoveryState.Error("Advertising start failed: ${e.message}")
        }
    }

    /**
     * Path B: LE Coded PHY (True Extended Range on Supported Hardware)
     */
    @SuppressLint("MissingPermission")
    private fun startExtendedCodedAdvertising() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startStandardAdvertising()
            return
        }

        val payloadBytes = buildSyncPacket()
        try {
            val parameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(false)
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
                .setSecondaryPhy(BluetoothDevice.PHY_LE_CODED)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .addServiceData(SYNC_SERVICE_PARCEL, payloadBytes)
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .build()

            advertiser?.startAdvertisingSet(
                parameters,
                advertiseData,
                null,
                null,
                null,
                extendedAdvertiseCallback
            )
            Log.i(TAG, "Launching LE Coded PHY Extended Advertising")
        } catch (e: Exception) {
            Log.w(TAG, "Coded PHY advertising rejected by HAL (${e.message}). Falling back to Standard...", e)
            startStandardAdvertising()
        }
    }

    /**
     * Start Scanner with Hardware ServiceData Filter & Power Optimization
     */
    @SuppressLint("MissingPermission")
    private fun startConfiguredScan(useExtendedPhy: Boolean) {
        // Hardware filters: Match on Service UUID directly (universal across chipsets)
        // Software verification in scanCallback inspects Service Data and protocol version
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(SYNC_SERVICE_PARCEL).build(),
            ScanFilter.Builder().setServiceData(SYNC_SERVICE_PARCEL, null).build()
        )

        val scanMode = if (targetPowerMode == RadarPowerMode.SMART_DISCOVERY) {
            ScanSettings.SCAN_MODE_BALANCED
        } else {
            ScanSettings.SCAN_MODE_LOW_LATENCY
        }

        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(scanMode)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)

        if (useExtendedPhy && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settingsBuilder.setLegacy(false)
            settingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        }

        try {
            scanner?.startScan(filters, settingsBuilder.build(), scanCallback)
            isScanning = true
            Log.i(TAG, "BLE Scanner active with Low Latency filter (Extended=$useExtendedPhy)")
            updateActiveState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scanner", e)
            isScanning = false
            _discoveryState.value = DiscoveryState.Error("Scan start failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertisingOnly() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                advertiser?.stopAdvertisingSet(extendedAdvertiseCallback)
            }
        } catch (_: Exception) {}
        try {
            advertiser?.stopAdvertising(legacyAdvertiseCallback)
        } catch (_: Exception) {}
        isAdvertising = false
    }

    fun stopRadar() {
        targetPowerMode = RadarPowerMode.OFF

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentAdvertisingSet != null) {
                advertiser?.stopAdvertisingSet(extendedAdvertiseCallback)
                currentAdvertisingSet = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping extended advertising: ${e.message}")
        }

        try {
            advertiser?.stopAdvertising(legacyAdvertiseCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping legacy advertising: ${e.message}")
        }

        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scanner: ${e.message}")
        }

        isAdvertising = false
        isScanning = false
        isExtendedActive = false
        _discoveryState.value = DiscoveryState.Idle
        Log.i(TAG, "All radar radios powered down cleanly")
    }

    fun destroy() {
        stopRadar()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                receiverRegistered = false
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver", e)
            }
        }
    }

    private fun updateActiveState() {
        if (isAdvertising || isScanning) {
            _discoveryState.value = DiscoveryState.Active(
                isAdvertising = isAdvertising,
                isScanning = isScanning,
                isExtendedPhy = isExtendedActive
            )
        }
    }

    /**
     * Builds the exact 9-byte Sync packet: [0x01 Version] + [8 Bytes Token]
     */
    private fun buildSyncPacket(): ByteArray {
        val payload = ByteArray(9)
        payload[0] = PROTOCOL_VERSION
        val tokenBytes = hexToBytes(currentTokenHex)
        for (i in 0 until 8) {
            payload[1 + i] = if (i < tokenBytes.size) tokenBytes[i] else 0
        }
        return payload
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace("-", "").trim()
        val len = clean.length
        val result = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            result[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
        }
        return result
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            sb.append(hexChars[i ushr 4])
            sb.append(hexChars[i and 0x0F])
        }
        return sb.toString()
    }

    fun getCurrentMode(): RadarPowerMode = targetPowerMode
}
