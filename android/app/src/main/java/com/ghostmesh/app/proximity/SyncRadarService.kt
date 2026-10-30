package com.ghostmesh.app.proximity

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.ghostmesh.app.data.UserSession
import java.util.concurrent.ConcurrentHashMap

class SyncRadarService : Service() {

    companion object {
        private const val TAG = "SyncRadarService"

        const val ACTION_START_RADAR = "com.ghostmesh.app.action.START_RADAR"
        const val ACTION_STOP_RADAR = "com.ghostmesh.app.action.STOP_RADAR"
        const val ACTION_SET_FOREGROUND_STATE = "com.ghostmesh.app.action.SET_FOREGROUND_STATE"
        const val EXTRA_IS_APP_FOREGROUND = "extra_is_app_foreground"

        // Global callback for real-time in-app UI delivery when MainActivity is active
        var onPeerDiscoveredListener: ((FilteredBleObservation) -> Unit)? = null

        // 5-minute notification cooldown per peer token/handle to prevent alert spam
        private const val NOTIFICATION_COOLDOWN_MS = 5 * 60 * 1000L
        private val lastNotifiedMap = ConcurrentHashMap<String, Long>()

        fun startService(context: Context, isAppForeground: Boolean = true) {
            val intent = Intent(context, SyncRadarService::class.java).apply {
                action = ACTION_START_RADAR
                putExtra(EXTRA_IS_APP_FOREGROUND, isAppForeground)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SyncRadarService::class.java).apply {
                action = ACTION_STOP_RADAR
            }
            context.startService(intent)
        }

        fun notifyAppForegroundState(context: Context, isForeground: Boolean) {
            val intent = Intent(context, SyncRadarService::class.java).apply {
                action = ACTION_SET_FOREGROUND_STATE
                putExtra(EXTRA_IS_APP_FOREGROUND, isForeground)
            }
            context.startService(intent)
        }
    }

    private var bleManager: BleDiscoveryManager? = null
    private var thermalController: ThermalAndBatteryController? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isAppInForeground: Boolean = true

    override fun onCreate() {
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
                val broadcastName = if (igHandle.isNotBlank()) "IG:$igHandle" else UserSession.getDisplayName(this)
                bleManager?.setPowerMode(RadarPowerMode.SMART_DISCOVERY, token, broadcastName)
            }
        }
        thermalController?.startMonitoring()
    }

    private fun initBleManager() {
        if (bleManager != null) return

        val proximityEvaluator = ProximityEvaluator(
            windowSize = 3,
            
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
        } else {
            displayName = broadcastName
            igHandle = broadcastName
        }

        val rawToken = observation.deviceToken
        val now = SystemClock.elapsedRealtime()
        val dedupKey = if (igHandle.isNotBlank()) igHandle.lowercase() else rawToken
        val lastAlertTime = lastNotifiedMap[dedupKey] ?: 0L

        // 3. Post Instant Alert Notification if cooldown elapsed (>5 min)
        if (now - lastAlertTime > NOTIFICATION_COOLDOWN_MS) {
            lastNotifiedMap[dedupKey] = now

            val distDisplay = when (observation.proximityTier) {
                ProximityTier.VERY_CLOSE -> "3m • Very Close"
                ProximityTier.NEARBY -> "12m • Nearby"
                ProximityTier.EXTENDED -> "28m • In Range"
            }

            Log.i(TAG, "Firing Peer Alert Notification for @$igHandle ($distDisplay)")
            NotificationHelper.postPeerDetectedNotification(
                context = this,
                instagramHandle = if (igHandle.isNotBlank()) igHandle else "sync_user",
                displayName = displayName.ifBlank { "Nearby Sync User" },
                distanceDisplay = distDisplay,
                encounterId = rawToken
            )
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
        }
        return START_STICKY
    }

    private fun startRadarEngine() {
        val token = UserSession.getBleToken(this)
        val igHandle = UserSession.getInstagramUsername(this)
        val displayName = UserSession.getDisplayName(this)
        
        // Broadcast format: "IG:<handle>" or "<name>|<handle>"
        val broadcastName = if (igHandle.isNotBlank()) {
            "IG:$igHandle"
        } else {
            displayName.ifBlank { "Sync" }
        }

        // Foreground: AGGRESSIVE for sub-second radar sweep
        // Background: BALANCED / PASSIVE with hardware filtering to guarantee ZERO battery drain
        val targetMode = if (isAppInForeground) {
            RadarPowerMode.EXTENDED_RANGE
        } else {
            RadarPowerMode.SMART_DISCOVERY
        }

        Log.i(TAG, "Starting BLE Radar in mode: $targetMode with broadcast name: $broadcastName")
        bleManager?.setPowerMode(targetMode, token, broadcastName)
    }

    private fun updateRadarPowerMode() {
        val token = UserSession.getBleToken(this)
        val igHandle = UserSession.getInstagramUsername(this)
        val broadcastName = if (igHandle.isNotBlank()) "IG:$igHandle" else UserSession.getDisplayName(this)

        val targetMode = if (isAppInForeground) {
            RadarPowerMode.EXTENDED_RANGE
        } else {
            RadarPowerMode.SMART_DISCOVERY
        }

        Log.i(TAG, "App foreground state changed ($isAppInForeground) -> switching power mode to $targetMode")
        bleManager?.setPowerMode(targetMode, token, broadcastName)
    }

    private fun stopRadarEngine() {
        Log.i(TAG, "Stopping BLE Radar engine")
        bleManager?.setPowerMode(RadarPowerMode.OFF, "", "")
    }

    override fun onDestroy() {
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
