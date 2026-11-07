package com.spott.app.proximity

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.spott.app.data.UserSession
import com.spott.app.data.network.SyncApiClient

import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class SyncRadarService : Service() {

    companion object {
        private const val TAG = "SyncRadarService"

        const val ACTION_START_RADAR = "com.spott.app.action.START_RADAR"
        const val ACTION_STOP_RADAR = "com.spott.app.action.STOP_RADAR"
        const val ACTION_SET_FOREGROUND_STATE = "com.spott.app.action.SET_FOREGROUND_STATE"
        const val EXTRA_IS_APP_FOREGROUND = "extra_is_app_foreground"
        const val ACTION_SET_GHOST_MODE = "com.spott.app.action.SET_GHOST_MODE"
        const val EXTRA_IS_GHOST_MODE = "extra_is_ghost_mode" 

        // Global callback for real-time in-app UI delivery when MainActivity is active
        var onPeerDiscoveredListener: ((FilteredBleObservation) -> Unit)? = null

        // 5-minute notification cooldown per peer token/handle to prevent alert spam
        private const val NOTIFICATION_COOLDOWN_MS = 5 * 60 * 1000L
        private val lastNotifiedMap = ConcurrentHashMap<String, Long>()

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context, isAppForeground: Boolean = true) {
            try {
                val intent = Intent(context, SyncRadarService::class.java).apply {
                    action = ACTION_START_RADAR
                    putExtra(EXTRA_IS_APP_FOREGROUND, isAppForeground)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isServiceRunning = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SyncRadarService: " + e.message, e)
            }
        }

        fun stopService(context: Context) {
            if (!isServiceRunning) return
            try {
                val intent = Intent(context, SyncRadarService::class.java).apply {
                    action = ACTION_STOP_RADAR
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop SyncRadarService: " + e.message)
            }
        }

        fun notifyAppForegroundState(context: Context, isForeground: Boolean) {
            if (!isServiceRunning) return
            try {
                val intent = Intent(context, SyncRadarService::class.java).apply {
                    action = ACTION_SET_FOREGROUND_STATE
                    putExtra(EXTRA_IS_APP_FOREGROUND, isForeground)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not notify foreground state: " + e.message)
            }
        }

        fun notifyGhostMode(context: Context, isGhost: Boolean) {
            if (!isServiceRunning) return
            try {
                val intent = Intent(context, SyncRadarService::class.java).apply {
                    action = ACTION_SET_GHOST_MODE
                    putExtra(EXTRA_IS_GHOST_MODE, isGhost)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not notify ghost mode: " + e.message)
            }
        }
    }

    private var bleManager: BleDiscoveryManager? = null
    private var thermalController: ThermalAndBatteryController? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isAppInForeground: Boolean = true
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiClient = SyncApiClient()

    override fun onCreate() {
        isServiceRunning = true
        super.onCreate()
        Log.i(TAG, "SyncRadarService onCreate: initializing background radar engine")

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sync:RadarPacketProcessor")

        // Start Foreground Service with notification
        val notification = NotificationHelper.buildServiceNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        // Initialize BLE Discovery Manager
        initBleManager()

        // Thermal & Battery Controller to guarantee zero overheating & zero battery drain
        thermalController = ThermalAndBatteryController(this) { reason, shouldPause ->
            Log.w(TAG, "Thermal/Battery throttle trigger: $reason, pause completely: $shouldPause")
            if (shouldPause) {
                bleManager?.setPowerMode(RadarPowerMode.OFF, "", "")
            } else {
                val token = UserSession.getBleToken(this)
                val igHandle = UserSession.getInstagramUsername(this)
                val displayName = UserSession.getDisplayName(this)
                bleManager?.setPowerMode(RadarPowerMode.SMART_DISCOVERY, token, displayName, igHandle)
            }
        }
        thermalController?.startMonitoring()
    }

    private fun initBleManager() {
        if (bleManager != null) return

        val proximityEvaluator = ProximityEvaluator(
            windowSize = 3,
            entryThresholdDbm = -105,
            minSightingsForExtended = 1,
            minSightingsForClose = 1
        )

        bleManager = BleDiscoveryManager(this, proximityEvaluator) { observation ->
            handleObservation(observation)
        }
    }

    private fun handleObservation(observation: FilteredBleObservation) {
        // Safely acquire partial wake-lock for quick notification dispatch
        wakeLock?.acquire(80L)

        // 1. Deliver to live in-app UI if MainActivity is actively open
        try {
            onPeerDiscoveredListener?.invoke(observation)
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking in-app listener", e)
        }

        // 2. Parse Instagram handle and name from over-the-air BLE broadcast
        val broadcastName = observation.broadcastDisplayName ?: ""
        var igHandle = ""
        var displayName = ""

        if (broadcastName.startsWith("IG:", ignoreCase = true)) {
            igHandle = broadcastName.substring(3).trim()
            displayName = igHandle
        } else if (broadcastName.contains("|")) {
            val parts = broadcastName.split("|")
            displayName = parts[0].trim()
            igHandle = parts.getOrNull(1)?.trim() ?: ""
        } else if (broadcastName.isNotBlank() && !BleDiscoveryManager.isDeviceHardwareName(broadcastName)) {
            displayName = broadcastName
            igHandle = ""
        } else {
            displayName = "Nearby Spott User"
            igHandle = ""
        }

        if (BleDiscoveryManager.isDeviceHardwareName(displayName)) {
            displayName = if (igHandle.isNotBlank()) igHandle else "Nearby Spott User"
        }

        val rawToken = observation.deviceToken
        val now = SystemClock.elapsedRealtime()
        val dedupKey = rawToken.lowercase()
        val lastAlertTime = lastNotifiedMap[dedupKey] ?: 0L

        // 3. Post Instant Heads-Up Alert Notification (5-min cooldown per device)
        if (now - lastAlertTime > NOTIFICATION_COOLDOWN_MS) {
            lastNotifiedMap[dedupKey] = now

            val distDisplay = when (observation.proximityTier) {
                ProximityTier.VERY_CLOSE -> "3m • Very Close"
                ProximityTier.NEARBY -> "12m • Nearby"
                ProximityTier.EXTENDED -> "28m • In Range"
            }

            serviceScope.launch {
                try {
                    val myUserId = UserSession.getUserId(this@SyncRadarService) ?: ""
                    var resolvedIg = igHandle
                    var resolvedName = displayName

                    // If over-the-air BLE packet only had token or missing handle, resolve identity via backend
                    if (resolvedIg.isBlank()) {
                        val res = apiClient.resolveBleToken(myUserId, rawToken)
                        val card = res.getOrNull()
                        if (card != null) {
                            resolvedIg = card.instagramUsername?.trim()?.removePrefix("@") ?: ""
                            if (card.displayName.isNotBlank() && !card.displayName.startsWith("Nearby Spott User")) {
                                resolvedName = card.displayName
                            }
                        }
                    }

                    if (resolvedName.isBlank() || BleDiscoveryManager.isDeviceHardwareName(resolvedName) || resolvedName.startsWith("Nearby Spott User")) {
                        resolvedName = if (resolvedIg.isNotBlank()) "@$resolvedIg" else "Nearby Spott User"
                    }

                    withContext(Dispatchers.Main) {
                        NotificationHelper.postPeerDetectedNotification(
                            context = this@SyncRadarService,
                            instagramHandle = resolvedIg,
                            displayName = resolvedName,
                            distanceDisplay = distDisplay,
                            encounterId = rawToken
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resolve and alert peer: " + e.message)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RADAR
        when (action) {
            ACTION_START_RADAR -> {
                isAppInForeground = intent?.getBooleanExtra(EXTRA_IS_APP_FOREGROUND, true) ?: true
                startRadarEngine()
            }
            ACTION_STOP_RADAR -> {
                stopRadarEngine()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SET_FOREGROUND_STATE -> {
                isAppInForeground = intent?.getBooleanExtra(EXTRA_IS_APP_FOREGROUND, true) ?: true
                updateRadarPowerMode()
            }
            ACTION_SET_GHOST_MODE -> {
                val ghost = intent?.getBooleanExtra(EXTRA_IS_GHOST_MODE, false) ?: false
                bleManager?.setGhostMode(ghost)
            }
        }
        return START_STICKY
    }

    private fun startRadarEngine() {
        val token = UserSession.getBleToken(this)
        val igHandle = UserSession.getInstagramUsername(this)
        val displayName = UserSession.getDisplayName(this)

        val targetMode = if (isAppInForeground) {
            RadarPowerMode.EXTENDED_RANGE
        } else {
            RadarPowerMode.SMART_DISCOVERY
        }

        Log.i(TAG, "Starting BLE Radar in mode: $targetMode (DisplayName: $displayName, IG: $igHandle)")
        bleManager?.setPowerMode(targetMode, token, displayName, igHandle)
        val isGhost = UserSession.isGhostMode(this) && UserSession.isAdmin(this)
        bleManager?.setGhostMode(isGhost)
    }

    private fun updateRadarPowerMode() {
        val token = UserSession.getBleToken(this)
        val igHandle = UserSession.getInstagramUsername(this)
        val displayName = UserSession.getDisplayName(this)

        val targetMode = if (isAppInForeground) {
            RadarPowerMode.EXTENDED_RANGE
        } else {
            RadarPowerMode.SMART_DISCOVERY
        }

        Log.i(TAG, "App foreground state changed ($isAppInForeground) -> switching power mode to $targetMode")
        bleManager?.setPowerMode(targetMode, token, displayName, igHandle)
    }

    private fun stopRadarEngine() {
        Log.i(TAG, "Stopping BLE Radar engine")
        bleManager?.setPowerMode(RadarPowerMode.OFF, "", "")
    }

    override fun onDestroy() {
        isServiceRunning = false
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "SyncRadarService onDestroy")
        stopRadarEngine()
        thermalController?.stopMonitoring()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
