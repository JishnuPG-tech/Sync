package com.ghostmesh.app

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ghostmesh.app.data.UserSession
import com.ghostmesh.app.data.model.AppearanceMode
import com.ghostmesh.app.data.model.ConnectionRequestIncoming
import com.ghostmesh.app.data.model.MutualConnection
import com.ghostmesh.app.data.model.NearbyPeerUiModel
import com.ghostmesh.app.data.model.ThemePreset
import com.ghostmesh.app.data.network.SyncApiClient
import com.ghostmesh.app.proximity.BleDiscoveryManager
import com.ghostmesh.app.proximity.ConfidenceCategory
import com.ghostmesh.app.proximity.ProximityEvaluator
import com.ghostmesh.app.proximity.ProximityTier
import com.ghostmesh.app.proximity.RadarPowerMode
import com.ghostmesh.app.proximity.ThermalAndBatteryController
import com.ghostmesh.app.ui.components.AuraBottomBar
import com.ghostmesh.app.ui.components.AuraTab
import com.ghostmesh.app.ui.screens.DiscoveredScreen
import com.ghostmesh.app.ui.screens.InstagramInputScreen
import com.ghostmesh.app.ui.screens.ProfileScreen
import com.ghostmesh.app.ui.screens.ProfileSetupScreen
import com.ghostmesh.app.ui.screens.RadarScreen
import com.ghostmesh.app.ui.screens.SplashScreen
import com.ghostmesh.app.ui.screens.WelcomeScreen
import com.ghostmesh.app.ui.theme.PlexTheme
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
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun checkLocPerms(): Boolean {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                   checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        // Request modern Android permissions on launch
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            var currentThemePreset by remember { mutableStateOf(ThemePreset.CLAUDE_WARM) }
            var appearanceMode by remember { mutableStateOf(AppearanceMode.LIGHT) }

            // Hardware & Permission state tracking
            var isBluetoothEnabled by remember {
                mutableStateOf(BluetoothAdapter.getDefaultAdapter()?.isEnabled == true)
            }
            val locManager = remember { getSystemService(Context.LOCATION_SERVICE) as? LocationManager }
            var isLocationEnabled by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        locManager?.isLocationEnabled == true
                    } else {
                        locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        locManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                    }
                )
            }
            var hasBluetoothPermission by remember { mutableStateOf(checkBtPerms()) }
            var hasLocationPermission by remember { mutableStateOf(checkLocPerms()) }

            // Dynamic listener for Bluetooth/Location state toggles & permission grants
            DisposableEffect(Unit) {
                onPermissionsUpdated = {
                    hasBluetoothPermission = checkBtPerms()
                    hasLocationPermission = checkLocPerms()
                }
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        when (intent?.action) {
                            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                                isBluetoothEnabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
                            }
                            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                                isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    locManager?.isLocationEnabled == true
                                } else {
                                    locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                                    locManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                                }
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                    addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
                }
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

            val enableLocation = {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (_: Exception) {}
            }

            val requestAllPermissions = {
                val perms = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    perms.add(Manifest.permission.BLUETOOTH_SCAN)
                    perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                    perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(perms.toTypedArray())
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

            // Collections
            val nearbyPeers = remember { mutableStateListOf<NearbyPeerUiModel>() }
            val blePeersMap = remember { mutableStateMapOf<String, NearbyPeerUiModel>() }
            val gpsPeersMap = remember { mutableStateMapOf<String, NearbyPeerUiModel>() }
            val discoveredPeers = remember { mutableStateListOf<NearbyPeerUiModel>() }
            val requestedPeerIds = remember { mutableStateListOf<String>() }
            val incomingRequests = remember { mutableStateListOf<ConnectionRequestIncoming>() }
            val mutualConnections = remember { mutableStateListOf<MutualConnection>() }

            val apiClient = remember { SyncApiClient() }
            val coroutineScope = rememberCoroutineScope()

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

            val bleManager = remember {
                BleDiscoveryManager(this@MainActivity, proximityEvaluator) { observation ->
                    if (powerMode == RadarPowerMode.OFF || currentUserId.isBlank()) return@BleDiscoveryManager
                    val rawToken = observation.deviceToken
                    val nowElapsed = SystemClock.elapsedRealtime()
                    lastSeenMonotonicMap[rawToken] = nowElapsed

                    // 1. In-Memory Cache Hit: Update RSSI, distance, and timestamps with ZERO network lookups
                    val cachedProfile = resolvedProfileCache[rawToken]
                    if (cachedProfile != null) {
                        val distDisplay = when (observation.proximityTier) {
                            ProximityTier.VERY_CLOSE -> "Very Close • 1-3m"
                            ProximityTier.NEARBY -> "Nearby • ~5m"
                            ProximityTier.EXTENDED -> "Extended • ~15m"
                        }
                        val updatedUi = cachedProfile.copy(
                            distanceDisplay = distDisplay,
                            isCurrentlyInRange = true,
                            confidenceTier = observation.proximityTier,
                            lastSeenElapsedMs = nowElapsed
                        )
                        blePeersMap[cachedProfile.encounterId] = updatedUi
                        updateCombinedPeers()
                        return@BleDiscoveryManager
                    }

                    // 2. Deduplicate network lookups: verify cooldown & in-flight state
                    val lastFailed = failedLookupCooldown[rawToken] ?: 0L
                    if (nowElapsed - lastFailed < 30_000L) {
                        return@BleDiscoveryManager
                    }
                    if (inFlightLookups[rawToken] == true) {
                        return@BleDiscoveryManager
                    }

                    inFlightLookups[rawToken] = true
                    coroutineScope.launch {
                        try {
                            val resolveRes = apiClient.resolveBleToken(currentUserId, rawToken)
                            if (resolveRes.isSuccess) {
                                val peerCard = resolveRes.getOrNull()
                                if (peerCard != null && peerCard.userId != currentUserId) {
                                    val distDisplay = when (observation.proximityTier) {
                                        ProximityTier.VERY_CLOSE -> "Very Close • 1-3m"
                                        ProximityTier.NEARBY -> "Nearby • ~5m"
                                        ProximityTier.EXTENDED -> "Extended • ~15m"
                                    }
                                    val peerUi = NearbyPeerUiModel(
                                        encounterId = peerCard.userId,
                                        displayName = peerCard.displayName,
                                        introduction = peerCard.bio,
                                        avatarAsset = peerCard.avatarUrl,
                                        distanceDisplay = distDisplay,
                                        isCurrentlyInRange = true,
                                        confidenceTier = observation.proximityTier,
                                        lastSeenElapsedMs = nowElapsed
                                    )
                                    resolvedProfileCache[rawToken] = peerUi
                                    tokenToUserIdMap[rawToken] = peerCard.userId
                                    blePeersMap[peerCard.userId] = peerUi
                                    updateCombinedPeers()
                                }
                            } else {
                                failedLookupCooldown[rawToken] = SystemClock.elapsedRealtime()
                                android.util.Log.w("MainActivity", "Failed to resolve BLE token $rawToken: ${resolveRes.exceptionOrNull()?.message}")
                            }
                        } finally {
                            inFlightLookups.remove(rawToken)
                        }
                    }
                }
            }

            // Clean up BLE manager resources on dispose
            DisposableEffect(bleManager) {
                onDispose {
                    bleManager.destroy()
                }
            }

            // Presence Expiry: Prune departed peers (absence > 15s) every 3 seconds
            LaunchedEffect(powerMode) {
                while (powerMode != RadarPowerMode.OFF) {
                    delay(3_000)
                    val now = SystemClock.elapsedRealtime()
                    var changed = false
                    val iterator = lastSeenMonotonicMap.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (now - entry.value > 15_000L) {
                            val userId = tokenToUserIdMap[entry.key]
                            if (userId != null) {
                                blePeersMap.remove(userId)
                                changed = true
                            }
                            iterator.remove()
                        }
                    }
                    if (changed) {
                        updateCombinedPeers()
                    }
                }
            }

            // Real physical continuous GPS location listener
            val locationListener = remember {
                object : android.location.LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (currentUserId.isNotBlank()) {
                            coroutineScope.launch {
                                apiClient.pingLocation(currentUserId, location.latitude, location.longitude)
                            }
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            }

            // Manage hardware BLE radios & live GPS listener based on powerMode & hardware toggles
            DisposableEffect(powerMode, currentUserId, isBluetoothEnabled, hasBluetoothPermission, isLocationEnabled, hasLocationPermission) {
                if (powerMode != RadarPowerMode.OFF && currentUserId.isNotBlank()) {
                    coroutineScope.launch {
                        apiClient.toggleDiscovery(currentUserId, true)
                    }

                    if (isBluetoothEnabled && hasBluetoothPermission) {
                        val tokenToAdvertise = UserSession.getBleToken(this@MainActivity)
                        bleManager.setPowerMode(powerMode, tokenToAdvertise)
                    }

                    if (locManager != null && isLocationEnabled && hasLocationPermission) {
                        // 1. Ping initial cached fix if available
                        try {
                            val last = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                ?: locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                            last?.let {
                                coroutineScope.launch {
                                    apiClient.pingLocation(currentUserId, it.latitude, it.longitude)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MainActivity", "Failed to get last known location", e)
                        }

                        // 2. Request active continuous updates with Looper.getMainLooper()
                        try {
                            locManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                5000L,
                                2f,
                                locationListener,
                                android.os.Looper.getMainLooper()
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("MainActivity", "GPS_PROVIDER unavailable", e)
                        }
                        try {
                            locManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                5000L,
                                2f,
                                locationListener,
                                android.os.Looper.getMainLooper()
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("MainActivity", "NETWORK_PROVIDER unavailable", e)
                        }
                    }
                } else {
                    if (currentUserId.isNotBlank()) {
                        coroutineScope.launch {
                            apiClient.toggleDiscovery(currentUserId, false)
                        }
                    }
                    bleManager.setPowerMode(RadarPowerMode.OFF, "")
                    blePeersMap.clear()
                    gpsPeersMap.clear()
                    updateCombinedPeers()
                    try {
                        locManager?.removeUpdates(locationListener)
                    } catch (_: Exception) {}
                }

                onDispose {
                    try {
                        locManager?.removeUpdates(locationListener)
                    } catch (_: Exception) {}
                }
            }

            // Synchronize with backend API
            fun fetchBackendData() {
                if (currentUserId.isBlank()) return
                coroutineScope.launch {
                    // 1. Fetch nearby peers from backend (strict real GPS haversine distance)
                    val peersRes = apiClient.getNearbyPeers(currentUserId)
                    if (peersRes.isSuccess) {
                        val peers = peersRes.getOrNull() ?: emptyList()
                        gpsPeersMap.clear()
                        peers.forEach { peer ->
                            if (peer.encounterId != currentUserId) {
                                gpsPeersMap[peer.encounterId] = peer
                            }
                        }
                        updateCombinedPeers()
                    }

                    // 2. Fetch pending requests
                    val reqsRes = apiClient.getPendingRequests(currentUserId)
                    if (reqsRes.isSuccess) {
                        val reqs = reqsRes.getOrNull() ?: emptyList()
                        incomingRequests.clear()
                        incomingRequests.addAll(reqs)
                    }

                    // 3. Fetch mutual connections
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
                                    currentSyncUsername = igHandle.replace(".", "_")
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
                                                isLocationEnabled = isLocationEnabled,
                                                hasBluetoothPermission = hasBluetoothPermission,
                                                hasLocationPermission = hasLocationPermission,
                                                onEnableBluetooth = enableBluetooth,
                                                onEnableLocation = enableLocation,
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
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
