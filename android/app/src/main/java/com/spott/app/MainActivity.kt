package com.spott.app

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

import com.spott.app.proximity.SyncRadarService
import com.spott.app.proximity.FilteredBleObservation
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.spott.app.data.UserSession
import com.spott.app.data.model.AppearanceMode
import com.spott.app.data.model.ConnectionRequestIncoming
import com.spott.app.data.model.MutualConnection
import com.spott.app.data.model.NearbyPeerUiModel
import com.spott.app.data.model.ThemePreset
import com.spott.app.data.network.SyncApiClient
import com.spott.app.proximity.BleDiscoveryManager
import com.spott.app.proximity.ConfidenceCategory
import com.spott.app.proximity.ProximityEvaluator
import com.spott.app.proximity.ProximityTier
import com.spott.app.proximity.RadarPowerMode
import com.spott.app.proximity.ThermalAndBatteryController
import com.spott.app.ui.components.AuraBottomBar
import com.spott.app.ui.components.AuraTab
import com.spott.app.ui.components.UpdateDialog
import com.spott.app.update.AppUpdateInfo
import com.spott.app.update.UpdateManager
import com.spott.app.ui.screens.DiscoveredScreen
import com.spott.app.ui.screens.InstagramInputScreen
import com.spott.app.ui.screens.ProfileScreen
import com.spott.app.ui.screens.ProfileSetupScreen
import com.spott.app.ui.screens.RadarScreen
import com.spott.app.ui.screens.SplashScreen
import com.spott.app.ui.screens.WelcomeScreen
import com.spott.app.ui.theme.PlexTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class AppNavState {
    SPLASH,
    WELCOME,
    INSTAGRAM_INPUT,
    SETUP,
    MAIN
}

class MainActivity : ComponentActivity() {


    private var onPermissionsUpdated: (() -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
        }
    }


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onPermissionsUpdated?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun checkBtPerms(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }

        // Permissions are requested smoothly via LaunchedEffect once the window is rendered

        setContent {
            // Smoothly request missing permissions once the splash UI has visibly drawn
            LaunchedEffect(Unit) {
                val missing = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.BLUETOOTH_SCAN)
                    }
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                    }
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                } else {
                    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (missing.isNotEmpty()) {
                    delay(400) // Ensure the splash screen is drawn on screen first
                    permissionLauncher.launch(missing.toTypedArray())
                }
            }

            var currentThemePreset by remember { mutableStateOf(ThemePreset.CLAUDE_WARM) }
            var appearanceMode by remember { mutableStateOf(AppearanceMode.SYSTEM) }

            // Hardware & Permission state tracking (Pure Bluetooth)
            var isBluetoothEnabled by remember {
                mutableStateOf(BluetoothAdapter.getDefaultAdapter()?.isEnabled == true)
            }
            var hasBluetoothPermission by remember { mutableStateOf(checkBtPerms()) }

            // Dynamic listener for Bluetooth state toggles & permission grants
            DisposableEffect(Unit) {
                onPermissionsUpdated = {
                    hasBluetoothPermission = checkBtPerms()
                }
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                            isBluetoothEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
                        }
                    }
                }
                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(receiver, filter)

                onDispose {
                    onPermissionsUpdated = null
                    try {
                        unregisterReceiver(receiver)
                    } catch (_: Exception) {}
                }
            }

            val enableBluetooth = {
                try {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (_: Exception) {}
                }
            }

            val requestAllPermissions = {
                val perms = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    perms.add(Manifest.permission.BLUETOOTH_SCAN)
                    perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                    perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (perms.isNotEmpty()) {
                    permissionLauncher.launch(perms.toTypedArray())
                }
            }

            // App Navigation State - if already registered, start at MAIN, otherwise SPLASH
            val isInitiallyRegistered = remember { UserSession.isRegistered(this@MainActivity) }
            var navState by remember { mutableStateOf(if (isInitiallyRegistered) AppNavState.MAIN else AppNavState.SPLASH) }

            // User Identity State - loaded from persistent session
            var currentUserId by remember { mutableStateOf(UserSession.getUserId(this@MainActivity) ?: "") }
            var currentDisplayName by remember { mutableStateOf(UserSession.getDisplayName(this@MainActivity)) }
            var currentSyncUsername by remember { mutableStateOf(UserSession.getSyncUsername(this@MainActivity)) }
            var currentInstagramHandle by remember { mutableStateOf(UserSession.getInstagramUsername(this@MainActivity)) }
            var currentIntro by remember { mutableStateOf(UserSession.getBio(this@MainActivity)) }
            var currentLocation by remember { mutableStateOf(UserSession.getLocation(this@MainActivity)) }
            var currentAvatarAsset by remember { mutableStateOf<String?>(UserSession.getAvatar(this@MainActivity)) }

            // Radar & Discovery State
            var powerMode by remember { mutableStateOf<RadarPowerMode>(RadarPowerMode.SMART_DISCOVERY) }
            var currentTab by remember { mutableStateOf(AuraTab.RADAR) }

            // Admin & Ghost Mode State
            var isAdmin by remember { mutableStateOf(UserSession.isAdmin(this@MainActivity)) }
            var isGhostMode by remember { mutableStateOf(UserSession.isGhostMode(this@MainActivity)) }

            val toggleGhostMode: (Boolean) -> Unit = { enabled ->
                isGhostMode = enabled
                UserSession.setGhostMode(this@MainActivity, enabled)
                SyncRadarService.notifyGhostMode(this@MainActivity, enabled)
            }

            val coroutineScope = rememberCoroutineScope()

            // In-App Auto-Updater State
            val currentAppVersion = com.spott.app.BuildConfig.VERSION_NAME
            var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
            var isDownloadingUpdate by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableFloatStateOf(0f) }
            var showUpdateDialog by remember { mutableStateOf(false) }

            val checkAppUpdates: (Boolean) -> Unit = { manual ->
                if (manual) {
                    android.widget.Toast.makeText(this@MainActivity, "Checking for updates...", android.widget.Toast.LENGTH_SHORT).show()
                }
                coroutineScope.launch {
                    val result = UpdateManager.checkForUpdate(currentAppVersion)
                    if (result.isSuccess) {
                        val update = result.getOrNull()
                        if (update != null) {
                            availableUpdate = update
                            showUpdateDialog = true
                        } else if (manual) {
                            android.widget.Toast.makeText(this@MainActivity, "You're on the latest version (v)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else if (manual) {
                        android.widget.Toast.makeText(this@MainActivity, "Couldn't check for updates. Check connection.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }


            // Silently check for GitHub updates 2 seconds after app launch
            LaunchedEffect(Unit) {
                delay(2000)
                checkAppUpdates(false)
            }

            // Collections
            val nearbyPeers = remember { mutableStateListOf<NearbyPeerUiModel>() }
            val blePeersMap = remember { mutableStateMapOf<String, NearbyPeerUiModel>() }
            val gpsPeersMap = remember { mutableStateMapOf<String, NearbyPeerUiModel>() }
            val discoveredPeers = remember { mutableStateListOf<NearbyPeerUiModel>() }
            val requestedPeerIds = remember { mutableStateListOf<String>() }
            val incomingRequests = remember { mutableStateListOf<ConnectionRequestIncoming>() }
            val mutualConnections = remember { mutableStateListOf<MutualConnection>() }

            val apiClient = remember { SyncApiClient() }

            // Merge BLE (real radio signals) and GPS (real physical distance) without clobbering each other
            fun updateCombinedPeers() {
                if (powerMode == RadarPowerMode.OFF) {
                    nearbyPeers.clear()
                    return
                }
                val combined = mutableListOf<NearbyPeerUiModel>()
                val seenIds = mutableSetOf<String>()

                // 1. Direct BLE radio sightings take top priority
                blePeersMap.values.forEach { peer ->
                    combined.add(peer)
                    seenIds.add(peer.encounterId)
                }

                // 2. Real GPS proximity sightings (< 70m)
                gpsPeersMap.values.forEach { peer ->
                    if (!seenIds.contains(peer.encounterId)) {
                        combined.add(peer)
                        seenIds.add(peer.encounterId)
                    }
                }

                nearbyPeers.clear()
                nearbyPeers.addAll(combined)

                // Accumulate into persistent discovered list (never deleted when radar pauses)
                combined.forEach { peer ->
                    val existingIdx = discoveredPeers.indexOfFirst { it.encounterId == peer.encounterId }
                    if (existingIdx >= 0) {
                        discoveredPeers[existingIdx] = peer
                    } else {
                        discoveredPeers.add(0, peer)
                    }
                }
            }

            val proximityEvaluator = remember { ProximityEvaluator() }

            // Profile Resolution Deduplication & Presence Tracking Caches
            val resolvedProfileCache = remember { ConcurrentHashMap<String, NearbyPeerUiModel>() }
            val inFlightLookups = remember { ConcurrentHashMap<String, Boolean>() }
            val failedLookupCooldown = remember { ConcurrentHashMap<String, Long>() }
            val tokenToUserIdMap = remember { ConcurrentHashMap<String, String>() }
            val lastSeenMonotonicMap = remember { ConcurrentHashMap<String, Long>() }

            fun processObservation(observation: FilteredBleObservation) {
                if (powerMode == RadarPowerMode.OFF) return
                val rawToken = observation.deviceToken
                val myBleToken = UserSession.getBleToken(this@MainActivity)
                if (rawToken.equals(myBleToken, ignoreCase = true)) return

                val nowElapsed = SystemClock.elapsedRealtime()
                lastSeenMonotonicMap[rawToken] = nowElapsed

                val distDisplay = when (observation.proximityTier) {
                    ProximityTier.VERY_CLOSE -> "Very Close • 1-3m"
                    ProximityTier.NEARBY -> "Nearby • ~5m"
                    ProximityTier.EXTENDED -> "In Range • ~12m"
                }

                val broadcastName = observation.broadcastDisplayName ?: ""
                var parsedIg: String? = null
                var parsedName: String

                if (broadcastName.startsWith("@")) {
                    parsedIg = broadcastName.removePrefix("@").trim()
                    parsedName = "@$parsedIg"
                } else if (broadcastName.startsWith("IG:", ignoreCase = true)) {
                    parsedIg = broadcastName.substring(3).trim()
                    parsedName = "@$parsedIg"
                } else if (broadcastName.contains("|")) {
                    val parts = broadcastName.split("|")
                    parsedName = parts[0].trim()
                    parsedIg = parts.getOrNull(1)?.trim()
                } else if (broadcastName.isNotBlank() && !BleDiscoveryManager.isDeviceHardwareName(broadcastName)) {
                    parsedName = broadcastName
                    parsedIg = null
                } else {
                    parsedName = "Nearby Spott User"
                    parsedIg = null
                }

                if (BleDiscoveryManager.isDeviceHardwareName(parsedName)) {
                    parsedName = parsedIg ?: "Nearby Spott User"
                }

                // 1. Existing Peer / In-Memory Cache Hit
                val provisionalId = tokenToUserIdMap[rawToken] ?: rawToken
                val existingPeer = blePeersMap[provisionalId] ?: resolvedProfileCache[rawToken]

                if (existingPeer != null) {
                    val finalIg = existingPeer.instagramUsername ?: parsedIg
                    val finalName = when {
                        existingPeer.displayName.isNotBlank() && !BleDiscoveryManager.isDeviceHardwareName(existingPeer.displayName) && !existingPeer.displayName.startsWith("Nearby Spott User") -> existingPeer.displayName
                        !finalIg.isNullOrBlank() -> "@${finalIg.removePrefix("@")}"
                        else -> parsedName
                    }
                    val updatedUi = existingPeer.copy(
                        displayName = finalName,
                        distanceDisplay = distDisplay,
                        isCurrentlyInRange = true,
                        confidenceTier = observation.proximityTier,
                        lastSeenElapsedMs = nowElapsed,
                        instagramUsername = finalIg
                    )
                    blePeersMap[provisionalId] = updatedUi
                    updateCombinedPeers()
                } else {
                    // 2. Instant Optimistic Render for new peer
                    val instantName = when {
                        parsedName.isNotBlank() && !BleDiscoveryManager.isDeviceHardwareName(parsedName) && !parsedName.startsWith("Nearby Spott User") -> parsedName
                        !parsedIg.isNullOrBlank() -> "@${parsedIg.removePrefix("@")}"
                        else -> "Nearby Spott User"
                    }
                    val instantUi = NearbyPeerUiModel(
                        encounterId = provisionalId,
                        displayName = instantName,
                        introduction = "Discovered in physical proximity via Bluetooth Low Energy",
                        avatarAsset = null,
                        distanceDisplay = distDisplay,
                        isCurrentlyInRange = true,
                        confidenceTier = observation.proximityTier,
                        lastSeenElapsedMs = nowElapsed,
                        instagramUsername = parsedIg
                    )
                    blePeersMap[provisionalId] = instantUi
                    updateCombinedPeers()
                }

                // 3. Fast Deduplicated Backend Metadata Enrichment (bio, photo avatar)
                val lastFailed = failedLookupCooldown[rawToken] ?: 0L
                if (nowElapsed - lastFailed < 4_000L) return
                if (inFlightLookups[rawToken] == true) return

                inFlightLookups[rawToken] = true
                coroutineScope.launch {
                    try {
                        val currentUid = UserSession.getUserId(this@MainActivity) ?: ""
                        val resolveRes = apiClient.resolveBleToken(currentUid, rawToken)
                        if (resolveRes.isSuccess) {
                            val peerCard = resolveRes.getOrNull()
                            if (peerCard != null && peerCard.userId != currentUid) {
                                val resolvedIg = peerCard.instagramUsername?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() } ?: parsedIg
                                val cleanDisplayName = when {
                                    peerCard.displayName.isNotBlank() && !BleDiscoveryManager.isDeviceHardwareName(peerCard.displayName) && !peerCard.displayName.startsWith("Nearby Spott User") -> peerCard.displayName
                                    !resolvedIg.isNullOrBlank() -> "@$resolvedIg"
                                    else -> parsedName
                                }
                                val peerUi = NearbyPeerUiModel(
                                    encounterId = peerCard.userId,
                                    displayName = cleanDisplayName,
                                    introduction = peerCard.bio,
                                    avatarAsset = peerCard.avatarUrl,
                                    distanceDisplay = distDisplay,
                                    isCurrentlyInRange = true,
                                    confidenceTier = observation.proximityTier,
                                    lastSeenElapsedMs = nowElapsed,
                                    instagramUsername = resolvedIg
                                )
                                resolvedProfileCache[rawToken] = peerUi
                                tokenToUserIdMap[rawToken] = peerCard.userId
                                if (provisionalId != peerCard.userId) {
                                    blePeersMap.remove(provisionalId)
                                }
                                blePeersMap[peerCard.userId] = peerUi
                                updateCombinedPeers()
                            }
                        } else {
                            failedLookupCooldown[rawToken] = SystemClock.elapsedRealtime()
                        }
                    } finally {
                        inFlightLookups.remove(rawToken)
                    }
                }
            }

            // Connect to SyncRadarService global stream
            DisposableEffect(Unit) {
                SyncRadarService.onPeerDiscoveredListener = { obs ->
                    runOnUiThread {
                        processObservation(obs)
                    }
                }
                onDispose {
                    SyncRadarService.onPeerDiscoveredListener = null
                }
            }

            // Presence Maintenance: Mark out of range gracefully and prune departed peers
            LaunchedEffect(powerMode) {
                while (powerMode != RadarPowerMode.OFF) {
                    delay(4_000)
                    val now = SystemClock.elapsedRealtime()
                    var changed = false
                    val iterator = lastSeenMonotonicMap.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        val token = entry.key
                        val lastSeen = entry.value
                        val elapsed = now - lastSeen

                        val targetId = tokenToUserIdMap[token] ?: token
                        val peer = blePeersMap[targetId]

                        if (peer != null) {
                            // If silent > 30s: mark as out of range, but keep visible in list
                            if (elapsed > 30_000L && peer.isCurrentlyInRange) {
                                blePeersMap[targetId] = peer.copy(
                                    isCurrentlyInRange = false,
                                    distanceDisplay = "Out of range"
                                )
                                changed = true
                            }
                            // If silent > 90s: peer fully departed, remove from active radar
                            if (elapsed > 90_000L) {
                                blePeersMap.remove(targetId)
                                iterator.remove()
                                changed = true
                            }
                        } else if (elapsed > 90_000L) {
                            iterator.remove()
                        }
                    }
                    if (changed) {
                        updateCombinedPeers()
                    }
                }
            }

            // Manage hardware BLE radios based on powerMode & hardware toggles (Pure BLE)
            DisposableEffect(powerMode, currentUserId, isBluetoothEnabled, hasBluetoothPermission) {
                if (powerMode != RadarPowerMode.OFF && currentUserId.isNotBlank()) {
                    coroutineScope.launch {
                        apiClient.toggleDiscovery(currentUserId, true)
                    }

                    if (isBluetoothEnabled && hasBluetoothPermission) {
                        SyncRadarService.startService(this@MainActivity, isAppForeground = true)
                    }
                } else if (powerMode == RadarPowerMode.OFF) {
                    if (currentUserId.isNotBlank()) {
                        coroutineScope.launch {
                            apiClient.toggleDiscovery(currentUserId, false)
                        }
                    }
                    SyncRadarService.stopService(this@MainActivity)
                    blePeersMap.clear()
                    gpsPeersMap.clear()
                    updateCombinedPeers()
                }

                onDispose {}
            }

            // Synchronize with backend API (Requests & Mutual Connections)
            fun fetchBackendData() {
                if (currentUserId.isBlank()) return
                coroutineScope.launch {
                    // 1. Fetch pending requests
                    val reqsRes = apiClient.getPendingRequests(currentUserId)
                    if (reqsRes.isSuccess) {
                        val reqs = reqsRes.getOrNull() ?: emptyList()
                        incomingRequests.clear()
                        incomingRequests.addAll(reqs)
                    }

                    // 2. Fetch mutual connections
                    val connRes = apiClient.getMutualConnections(currentUserId)
                    if (connRes.isSuccess) {
                        val conns = connRes.getOrNull() ?: emptyList()
                        mutualConnections.clear()
                        mutualConnections.addAll(conns)
                    }
                }
            }

            // Sync with backend: poll every 5s when radar active
            LaunchedEffect(navState, currentUserId, powerMode) {
                if (navState == AppNavState.MAIN && currentUserId.isNotBlank()) {
                    fetchBackendData()
                    while (powerMode != RadarPowerMode.OFF) {
                        delay(5_000)
                        fetchBackendData()
                    }
                }
            }

            // Thermal & Battery monitoring
            DisposableEffect(Unit) {
                val thermalController = ThermalAndBatteryController(this@MainActivity) { _, shouldPause ->
                    if (shouldPause) {
                        powerMode = RadarPowerMode.OFF
                        nearbyPeers.clear()
                    } else if (powerMode == RadarPowerMode.EXTENDED_RANGE) {
                        powerMode = RadarPowerMode.SMART_DISCOVERY
                    }
                }
                thermalController.startMonitoring()
                onDispose {
                    thermalController.stopMonitoring()
                }
            }

            // Handle back presses
            BackHandler(enabled = navState != AppNavState.SPLASH) {
                when (navState) {
                    AppNavState.WELCOME -> finish()
                    AppNavState.INSTAGRAM_INPUT -> navState = AppNavState.WELCOME
                    AppNavState.SETUP -> navState = AppNavState.INSTAGRAM_INPUT
                    AppNavState.MAIN -> {
                        if (currentTab != AuraTab.RADAR) {
                            currentTab = AuraTab.RADAR
                        } else {
                            finish()
                        }
                    }
                    else -> {}
                }
            }

            PlexTheme(
                preset = currentThemePreset,
                appearanceMode = appearanceMode
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (navState) {
                        AppNavState.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = { navState = AppNavState.WELCOME }
                            )
                        }

                        AppNavState.WELCOME -> {
                            WelcomeScreen(
                                onGetStarted = { navState = AppNavState.INSTAGRAM_INPUT }
                            )
                        }

                        AppNavState.INSTAGRAM_INPUT -> {
                            InstagramInputScreen(
                                initialHandle = currentInstagramHandle,
                                onContinue = { igHandle, igFullName, igAvatar, igBio ->
                                    currentInstagramHandle = igHandle
                                    if (!igFullName.isNullOrBlank()) {
                                        currentDisplayName = igFullName
                                    }
                                    currentSyncUsername = igHandle // Exact match to Instagram username
                                    if (!igAvatar.isNullOrBlank()) {
                                        currentAvatarAsset = igAvatar
                                    }
                                    if (!igBio.isNullOrBlank()) {
                                        currentIntro = igBio.take(60)
                                    }
                                    navState = AppNavState.SETUP
                                },
                                onSkip = {
                                    navState = AppNavState.SETUP
                                }
                            )
                        }

                        AppNavState.SETUP -> {
                            ProfileSetupScreen(
                                initialDisplayName = currentDisplayName,
                                initialSyncUsername = currentSyncUsername,
                                initialInstagramHandle = currentInstagramHandle,
                                initialBio = currentIntro,
                                initialLocation = currentLocation,
                                initialAvatar = currentAvatarAsset,
                                onProfileCreated = { userId, name, syncUname, insta, intro, loc, avatar ->
                                    currentUserId = userId
                                    currentDisplayName = name
                                    currentSyncUsername = syncUname
                                    currentInstagramHandle = insta
                                    currentIntro = intro
                                    currentLocation = loc
                                    currentAvatarAsset = avatar
                                    powerMode = RadarPowerMode.SMART_DISCOVERY
                                    navState = AppNavState.MAIN
                                    SyncRadarService.startService(this@MainActivity, isAppForeground = true)
                                    fetchBackendData()
                                }
                            )
                        }

                        AppNavState.MAIN -> {
                            Scaffold(
                                bottomBar = {
                                    // Strictly 3 tabs: RADAR, DISCOVERED, PROFILE
                                    AuraBottomBar(
                                        currentTab = currentTab,
                                        onTabSelected = {
                                            currentTab = it
                                            fetchBackendData()
                                        },
                                        pendingRequestsCount = incomingRequests.size
                                    )
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    when (currentTab) {
                                        AuraTab.RADAR -> {
                                            RadarScreen(
                                                currentUserName = currentDisplayName,
                                                currentUserAvatar = currentAvatarAsset,
                                                powerMode = powerMode,
                                                onPowerModeChanged = { newMode ->
                                                    powerMode = newMode
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.toggleDiscovery(currentUserId, newMode != RadarPowerMode.OFF)
                                                            if (newMode != RadarPowerMode.OFF) {
                                                                fetchBackendData()
                                                            }
                                                        }
                                                    }
                                                },
                                                nearbyPeers = nearbyPeers,
                                                requestedPeerIds = requestedPeerIds.toSet(),
                                                onSendConnect = { peer ->
                                                    if (!requestedPeerIds.contains(peer.encounterId)) {
                                                        requestedPeerIds.add(peer.encounterId)
                                                    }
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.sendConnectionRequest(currentUserId, peer.encounterId)
                                                            fetchBackendData()
                                                        }
                                                    }
                                                },
                                                onSkipPeer = { peer ->
                                                    nearbyPeers.remove(peer)
                                                },
                                                onRefreshPeers = {
                                                    fetchBackendData()
                                                },
                                                isBluetoothEnabled = isBluetoothEnabled,
                                                hasBluetoothPermission = hasBluetoothPermission,
                                                onEnableBluetooth = enableBluetooth,
                                                onRequestPermissions = requestAllPermissions
                                            )
                                        }

                                        AuraTab.DISCOVERED -> {
                                            DiscoveredScreen(
                                                nearbyPeers = discoveredPeers,
                                                incomingRequests = incomingRequests,
                                                requestedPeerIds = requestedPeerIds.toSet(),
                                                onAcceptRequest = { req ->
                                                    incomingRequests.remove(req)
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.respondToRequest(req.id, "ACCEPT", currentUserId)
                                                            fetchBackendData()
                                                        }
                                                    }
                                                },
                                                onDeclineRequest = { req ->
                                                    incomingRequests.remove(req)
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.respondToRequest(req.id, "DECLINE", currentUserId)
                                                        }
                                                    }
                                                },
                                                onSendConnect = { peer ->
                                                    if (!requestedPeerIds.contains(peer.encounterId)) {
                                                        requestedPeerIds.add(peer.encounterId)
                                                    }
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.sendConnectionRequest(currentUserId, peer.encounterId)
                                                            fetchBackendData()
                                                        }
                                                    }
                                                },
                                                onSkipPeer = { peer ->
                                                    discoveredPeers.remove(peer)
                                                    nearbyPeers.remove(peer)
                                                },
                                                onRefresh = {
                                                    fetchBackendData()
                                                }
                                            )
                                        }

                                        AuraTab.PROFILE -> {
                                            ProfileScreen(
                                                displayName = currentDisplayName.takeIf { it.isNotEmpty() } ?: "User",
                                                syncUsername = currentSyncUsername,
                                                instagramHandle = currentInstagramHandle,
                                                introduction = currentIntro,
                                                location = currentLocation,
                                                avatarAsset = currentAvatarAsset,
                                                encountersCount = discoveredPeers.size,
                                                connectedCount = mutualConnections.size,
                                                powerMode = powerMode,
                                                onPowerModeChanged = { newMode ->
                                                    powerMode = newMode
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.toggleDiscovery(currentUserId, newMode != RadarPowerMode.OFF)
                                                            if (newMode != RadarPowerMode.OFF) {
                                                                fetchBackendData()
                                                            }
                                                        }
                                                    }
                                                },
                                                themePreset = currentThemePreset,
                                                onThemePresetChanged = { currentThemePreset = it },
                                                appearanceMode = appearanceMode,
                                                onAppearanceChanged = { appearanceMode = it },
                                                onEditProfileClick = {
                                                    navState = AppNavState.SETUP
                                                },
                                                onRerunOnboarding = {
                                                    UserSession.clear(this@MainActivity)
                                                    currentUserId = ""
                                                    currentDisplayName = ""
                                                    currentSyncUsername = ""
                                                    currentInstagramHandle = ""
                                                    currentIntro = ""
                                                    currentAvatarAsset = null
                                                    nearbyPeers.clear()
                                                    discoveredPeers.clear()
                                                    navState = AppNavState.INSTAGRAM_INPUT
                                                },
                                                isAdmin = isAdmin,
                                                isGhostMode = isGhostMode,
                                                onGhostModeToggle = toggleGhostMode,
                                                onAdminUnlocked = { isAdmin = true },
                                                onCheckUpdates = { checkAppUpdates(true) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showUpdateDialog && availableUpdate != null) {
                        UpdateDialog(
                            updateInfo = availableUpdate!!,
                            isDownloading = isDownloadingUpdate,
                            downloadProgress = downloadProgress,
                            onUpdateClick = {
                                isDownloadingUpdate = true
                                coroutineScope.launch {
                                    val result = UpdateManager.downloadApk(
                                        context = this@MainActivity,
                                        downloadUrl = availableUpdate!!.downloadUrl,
                                        onProgress = { p -> downloadProgress = p }
                                    )
                                    isDownloadingUpdate = false
                                    if (result.isSuccess) {
                                        showUpdateDialog = false
                                        UpdateManager.launchInstall(this@MainActivity, result.getOrNull()!!)
                                    } else {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Download failed: " + result.exceptionOrNull()?.message,
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            onDismiss = { showUpdateDialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (SyncRadarService.isServiceRunning) {
            SyncRadarService.notifyAppForegroundState(this, true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (SyncRadarService.isServiceRunning) {
            SyncRadarService.notifyAppForegroundState(this, false)
        }
    }
}
