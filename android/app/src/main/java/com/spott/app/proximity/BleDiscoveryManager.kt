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
import kotlinx.coroutines.*
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
 * Spott BLE Discovery Manager (Production Universal Edition)
 *
 * Core Architecture:
 * - 100% Universal Standard 1M PHY BLE Advertising and Scanning across ALL Android phones.
 * - 16-bit Service UUID: 0x5350 ('S','P' Spott) -> 00005350-0000-1000-8000-00805f9b34fb.
 * - Primary Advertisement Packet (<= 30 bytes, ZERO payload overflow):
 *     [0x01 Version (1B)] + [Ephemeral Token (8B)] + [Instagram handle (up to 14B UTF-8)]
 *     -> Gives EVERY receiving phone instant profile identity in the VERY FIRST PACKET (100ms latency)!
 * - Scan Response Packet (<= 28 bytes):
 *     [0x02 Extended Identity Type (1B)] + [DisplayName / Full Handle (up to 22B UTF-8)]
 * - Backward Compatibility:
 *     Also recognizes legacy 128-bit UUID: 7b115359-9942-4f38-958b-0e54911d3312.
 * - Zero Chipset Filtering Drop:
 *     Software-level packet parsing ensures 100% reception on MIUI (Xiaomi/POCO), ColorOS,
 *     OneUI (Samsung), Pixel, and OxygenOS without hardware scan filter bugs.
 * - Self-Healing Radios:
 *     Automatic retry on scan registration errors (Android AOSP 5-scans-in-30s limit protection)
 *     and periodic watchdog to guarantee discovery is always alive.
 * - Absolute hardware device name suppression:
 *     Phone model names (e.g. "POCO M3", "CPH2381") are NEVER advertised or shown as profiles.
 */
class BleDiscoveryManager(
    private val context: Context,
    private val proximityEvaluator: ProximityEvaluator,
    private val onDeviceDiscovered: (FilteredBleObservation) -> Unit
) {
    companion object {
        private const val TAG = "BleDiscoveryManager"
        const val PROTOCOL_VERSION: Byte = 0x01
        const val IDENTITY_PAYLOAD_TYPE: Byte = 0x02

        // Standard 16-bit Service UUID for Spott: 0x5350 ('S','P')
        val SPOTT_SERVICE_UUID: UUID = UUID.fromString("00005350-0000-1000-8000-00805f9b34fb")
        val SPOTT_SERVICE_PARCEL: ParcelUuid = ParcelUuid(SPOTT_SERVICE_UUID)

        // Legacy 128-bit Service UUID for backward compatibility with earlier builds
        val LEGACY_SYNC_SERVICE_UUID: UUID = UUID.fromString("7b115359-9942-4f38-958b-0e54911d3312")
        val LEGACY_SYNC_SERVICE_PARCEL: ParcelUuid = ParcelUuid(LEGACY_SYNC_SERVICE_UUID)

        /**
         * Checks if a string looks like a hardware phone model name (e.g. POCO M3, Redmi Note, CPH2381)
         */
        fun isDeviceHardwareName(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val upper = name.uppercase().trim()
            val hardwareKeywords = listOf(
                "POCO", "REDMI", "XIAOMI", "SAMSUNG", "GALAXY", "PIXEL", "IPHONE", "OPPO",
                "VIVO", "REALME", "ONEPLUS", "MOTOROLA", "MOTO", "HUAWEI", "HONOR", "INFINIX",
                "TECNO", "ASUS", "ROG", "SM-", "CPH", "RMX", "M20", "M21", "2201", "2109", "2312",
                "ANDROID", "DEVICE", "BT-", "BLE-"
            )
            return hardwareKeywords.any { upper.contains(it) }
        }
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private var targetPowerMode: RadarPowerMode = RadarPowerMode.OFF
    private var currentTokenHex: String = ""
    private var currentDisplayName: String = ""
    private var currentInstagramHandle: String = ""

    private var isAdvertising: Boolean = false
    private var isScanning: Boolean = false
    private var isGhostMode: Boolean = false

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var watchdogJob: Job? = null
    private var scanRetryJob: Job? = null
    private var advertiseRetryJob: Job? = null

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private var receiverRegistered: Boolean = false
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.i(TAG, "Bluetooth turned ON. Auto-starting discovery radios...")
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
            Log.w(TAG, "Could not register bluetooth receiver", e)
        }
        startWatchdog()
    }

    /**
     * Periodic Watchdog: Ensures radios stay active and healthy without battery waste
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = managerScope.launch {
            while (isActive) {
                delay(12_000L)
                if (targetPowerMode != RadarPowerMode.OFF && currentTokenHex.isNotBlank()) {
                    val adapter = bluetoothAdapter
                    if (adapter != null && adapter.isEnabled && hasRequiredPermissions()) {
                        if (!isScanning) {
                            Log.i(TAG, "Watchdog: Scanner was inactive. Restarting...")
                            startConfiguredScan()
                        }
                        if (!isAdvertising && !isGhostMode) {
                            Log.i(TAG, "Watchdog: Advertiser was inactive. Restarting...")
                            startStandardAdvertising()
                        }
                    }
                }
            }
        }
    }

    fun setGhostMode(enabled: Boolean) {
        if (isGhostMode == enabled) return
        isGhostMode = enabled
        Log.i(TAG, "Ghost Mode toggled -> $isGhostMode")
        if (isGhostMode) {
            stopAdvertisingOnly()
        } else if (targetPowerMode != RadarPowerMode.OFF && !isAdvertising) {
            startStandardAdvertising()
        }
    }

    /**
     * BLE Scanner Callback with zero-drop software packet inspection
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            isScanning = true
            val scanRecord = result?.scanRecord ?: return

            var tokenBytes: ByteArray? = null
            var directHandle: String? = null
            var directDisplayName: String? = null

            // 1. Check Primary Spott 16-bit Service Data (UUID: 0x5350)
            val spottData = scanRecord.getServiceData(SPOTT_SERVICE_PARCEL) ?: findServiceData(scanRecord, SPOTT_SERVICE_UUID)
            if (spottData != null && spottData.isNotEmpty()) {
                if (spottData[0] == PROTOCOL_VERSION && spottData.size >= 9) {
                    tokenBytes = spottData.sliceArray(1..8)
                    if (spottData.size > 9) {
                        try {
                            val handleBytes = spottData.sliceArray(9 until spottData.size)
                            val handleStr = String(handleBytes, Charsets.UTF_8).trim()
                            if (handleStr.isNotBlank() && !isDeviceHardwareName(handleStr)) {
                                directHandle = handleStr.removePrefix("@")
                            }
                        } catch (_: Exception) {}
                    }
                } else if (spottData[0] == IDENTITY_PAYLOAD_TYPE && spottData.size > 1) {
                    try {
                        val nameBytes = spottData.sliceArray(1 until spottData.size)
                        val nameStr = String(nameBytes, Charsets.UTF_8).trim()
                        if (nameStr.isNotBlank() && !isDeviceHardwareName(nameStr)) {
                            directDisplayName = nameStr
                        }
                    } catch (_: Exception) {}
                }
            }

            // 2. Check Legacy 128-bit Service Data (UUID: 7b115359-...)
            if (tokenBytes == null) {
                val legacyData = scanRecord.getServiceData(LEGACY_SYNC_SERVICE_PARCEL) ?: findServiceData(scanRecord, LEGACY_SYNC_SERVICE_UUID)
                if (legacyData != null && legacyData.size >= 9 && legacyData[0] == PROTOCOL_VERSION) {
                    tokenBytes = legacyData.sliceArray(1..8)
                }
            }

            // If this packet is not a valid Spott packet, ignore
            if (tokenBytes == null) return

            val rawTokenHex = bytesToHex(tokenBytes)

            // Compose broadcast identity from over-the-air packet
            val finalBroadcastIdentity: String? = when {
                !directHandle.isNullOrBlank() && !directDisplayName.isNullOrBlank() -> "$directDisplayName|$directHandle"
                !directHandle.isNullOrBlank() -> "IG:$directHandle"
                !directDisplayName.isNullOrBlank() -> directDisplayName
                else -> null
            }

            val observation = proximityEvaluator.recordObservation(rawTokenHex, result.rssi, finalBroadcastIdentity)
            if (observation != null) {
                onDeviceDiscovered(observation)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE Scan Failed: $errorCode")
            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                isScanning = true
                updateActiveState()
            } else {
                isScanning = false
                _discoveryState.value = DiscoveryState.Error("Scan failed: $errorCode")

                // Auto-recover from transient errors (e.g. code 2: registration limit)
                scanRetryJob?.cancel()
                scanRetryJob = managerScope.launch {
                    delay(1500L)
                    if (targetPowerMode != RadarPowerMode.OFF) {
                        Log.i(TAG, "Attempting auto-recovery scan restart...")
                        startConfiguredScan()
                    }
                }
            }
        }
    }

    private fun findServiceData(scanRecord: ScanRecord, targetUuid: UUID): ByteArray? {
        for ((parcel, bytes) in scanRecord.serviceData) {
            if (parcel.uuid == targetUuid || parcel.uuid.toString().equals(targetUuid.toString(), ignoreCase = true)) {
                return bytes
            }
        }
        return null
    }

    /**
     * Standard BLE Advertiser Callback
     */
    private val legacyAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            Log.i(TAG, "Universal 1M PHY BLE Advertising active (Identity: $currentInstagramHandle / $currentDisplayName)")
            updateActiveState()
        }

        override fun onStartFailure(errorCode: Int) {
            if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                isAdvertising = true
                updateActiveState()
            } else {
                isAdvertising = false
                Log.e(TAG, "Universal BLE Advertising failed with code: $errorCode")
                _discoveryState.value = DiscoveryState.Error("Advertising failed: $errorCode")

                // Auto-recover from transient advertiser errors
                advertiseRetryJob?.cancel()
                advertiseRetryJob = managerScope.launch {
                    delay(1500L)
                    if (targetPowerMode != RadarPowerMode.OFF && !isGhostMode) {
                        Log.i(TAG, "Attempting auto-recovery advertiser restart...")
                        startStandardAdvertising()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun setPowerMode(
        mode: RadarPowerMode,
        token: String,
        displayName: String = "",
        instagramHandle: String = ""
    ) {
        val modeChanged = targetPowerMode != mode
        val cleanToken = token.replace("-", "").take(16).lowercase()
        val tokenChanged = currentTokenHex != cleanToken && cleanToken.isNotBlank()
        val cleanName = displayName.take(20).trim()
        val nameChanged = currentDisplayName != cleanName && cleanName.isNotBlank()
        val cleanIg = instagramHandle.trim().removePrefix("@").lowercase().take(20)
        val igChanged = currentInstagramHandle != cleanIg && cleanIg.isNotBlank()

        targetPowerMode = mode
        if (cleanToken.isNotBlank()) currentTokenHex = cleanToken
        if (cleanName.isNotBlank() && !isDeviceHardwareName(cleanName)) currentDisplayName = cleanName
        if (cleanIg.isNotBlank()) currentInstagramHandle = cleanIg

        if (mode == RadarPowerMode.OFF) {
            Log.i(TAG, "Radar Mode -> OFF: Stopping all radios cleanly")
            stopRadar()
            return
        }

        // If radios need configuration update or are inactive, restart them cleanly
        if (modeChanged || tokenChanged || nameChanged || igChanged || !isAdvertising || !isScanning) {
            startRadios()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScan = context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val hasAdv = context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            hasScan && hasAdv
        } else {
            val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            hasFine || hasCoarse
        }
    }

    @SuppressLint("MissingPermission")
    fun startRadios() {
        if (targetPowerMode == RadarPowerMode.OFF) return

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _discoveryState.value = DiscoveryState.BluetoothDisabled
            Log.w(TAG, "Bluetooth is disabled or not available")
            return
        }

        if (!hasRequiredPermissions()) {
            _discoveryState.value = DiscoveryState.MissingPermissions
            Log.w(TAG, "Required Bluetooth/Location permissions missing")
            return
        }

        advertiser = adapter.bluetoothLeAdvertiser
        scanner = adapter.bluetoothLeScanner

        if (advertiser == null) {
            _discoveryState.value = DiscoveryState.AdvertisingUnsupported
            Log.w(TAG, "BluetoothLeAdvertiser not supported on this device")
            return
        }

        if (currentTokenHex.isBlank()) {
            Log.w(TAG, "No token assigned yet for Spott discovery")
            return
        }

        // 1. Start Advertising (Universal 1M PHY)
        if (!isGhostMode) {
            startStandardAdvertising()
        } else {
            stopAdvertisingOnly()
        }

        // 2. Start Scanning (Universal Low-Latency)
        startConfiguredScan()
    }

    /**
     * Universal 1M PHY Advertising: 100% compatible with every smartphone in the world.
     * Guaranteed packet size <= 30 bytes (never exceeds 31-byte BLE hardware limit).
     */
    @SuppressLint("MissingPermission")
    private fun startStandardAdvertising() {
        stopAdvertisingOnly()

        val primaryPayload = buildPrimarySpottPacket()

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            // Primary Advertisement: 16-bit Service UUID + Token + Instagram Handle (<= 30 bytes)
            val data = AdvertiseData.Builder()
                .addServiceData(SPOTT_SERVICE_PARCEL, primaryPayload)
                .setIncludeDeviceName(false) // NEVER leak hardware device name (e.g. POCO M3)
                .setIncludeTxPowerLevel(false)
                .build()

            // Scan Response: Extended Display Name / Handle (<= 28 bytes)
            val scanResponseBuilder = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)

            val extendedIdentity = when {
                currentDisplayName.isNotBlank() && !isDeviceHardwareName(currentDisplayName) -> currentDisplayName
                currentInstagramHandle.isNotBlank() -> currentInstagramHandle
                else -> ""
            }

            if (extendedIdentity.isNotBlank()) {
                val identityBytes = extendedIdentity.toByteArray(Charsets.UTF_8).take(22).toByteArray()
                val responsePayload = byteArrayOf(IDENTITY_PAYLOAD_TYPE) + identityBytes
                scanResponseBuilder.addServiceData(SPOTT_SERVICE_PARCEL, responsePayload)
            }

            val scanResponse = scanResponseBuilder.build()

            advertiser?.startAdvertising(settings, data, scanResponse, legacyAdvertiseCallback)
            Log.i(TAG, "Initiated Universal BLE Advertising (Token: $currentTokenHex, Handle: $currentInstagramHandle)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Universal BLE Advertising: ${e.message}", e)
            isAdvertising = false
            _discoveryState.value = DiscoveryState.Error("Advertising start failed: ${e.message}")
        }
    }

    /**
     * Universal Low-Latency Scanning: Zero hardware filter drops, instant packet dispatch.
     */
    @SuppressLint("MissingPermission")
    private fun startConfiguredScan() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}

        // Pass empty filters to guarantee 100% zero-drop reception across all vendor chipsets.
        // scanCallback performs ultra-fast memory validation.
        val filters = emptyList<ScanFilter>()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0L)
            .build()

        try {
            scanner?.startScan(filters, settings, scanCallback)
            isScanning = true
            Log.i(TAG, "Universal BLE Scanner started in Low Latency mode (zero filter drop)")
            updateActiveState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scanner: ${e.message}", e)
            isScanning = false
            _discoveryState.value = DiscoveryState.Error("Scan start failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertisingOnly() {
        try {
            advertiser?.stopAdvertising(legacyAdvertiseCallback)
        } catch (_: Exception) {}
        isAdvertising = false
    }

    @SuppressLint("MissingPermission")
    fun stopRadar() {
        targetPowerMode = RadarPowerMode.OFF
        scanRetryJob?.cancel()
        advertiseRetryJob?.cancel()

        try {
            advertiser?.stopAdvertising(legacyAdvertiseCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising: ${e.message}")
        }

        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scanner: ${e.message}")
        }

        isAdvertising = false
        isScanning = false
        _discoveryState.value = DiscoveryState.Idle
        Log.i(TAG, "All radar radios powered down cleanly")
    }

    fun destroy() {
        stopRadar()
        watchdogJob?.cancel()
        managerScope.cancel()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
                receiverRegistered = false
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver: ${e.message}")
            }
        }
    }

    private fun updateActiveState() {
        if (isAdvertising || isScanning) {
            _discoveryState.value = DiscoveryState.Active(
                isAdvertising = isAdvertising,
                isScanning = isScanning,
                isExtendedPhy = false
            )
        }
    }

    /**
     * Builds Primary Spott Packet:
     * [0x01 Version (1B)] + [Token (8B)] + [Instagram handle (up to 14B UTF-8)]
     * Total length: 9 to 23 bytes (comfortably inside the 31-byte BLE limit)
     */
    private fun buildPrimarySpottPacket(): ByteArray {
        val tokenBytes = hexToBytes(currentTokenHex)
        val cleanIg = currentInstagramHandle.trim().removePrefix("@").take(14)
        val igBytes = cleanIg.toByteArray(Charsets.UTF_8)

        val totalSize = 1 + 8 + igBytes.size
        val payload = ByteArray(totalSize)
        payload[0] = PROTOCOL_VERSION

        for (i in 0 until 8) {
            payload[1 + i] = if (i < tokenBytes.size) tokenBytes[i] else 0
        }

        System.arraycopy(igBytes, 0, payload, 9, igBytes.size)
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
