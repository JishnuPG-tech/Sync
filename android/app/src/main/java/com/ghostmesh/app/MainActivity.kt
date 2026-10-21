package com.ghostmesh.app

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import com.ghostmesh.app.proximity.RadarPowerMode
import com.ghostmesh.app.proximity.ThermalAndBatteryController
import com.ghostmesh.app.ui.components.AuraBottomBar
import com.ghostmesh.app.ui.components.AuraTab
import com.ghostmesh.app.ui.screens.DiscoveredScreen
import com.ghostmesh.app.ui.screens.ProfileScreen
import com.ghostmesh.app.ui.screens.ProfileSetupScreen
import com.ghostmesh.app.ui.screens.RadarScreen
import com.ghostmesh.app.ui.screens.SplashScreen
import com.ghostmesh.app.ui.screens.WelcomeScreen
import com.ghostmesh.app.ui.theme.PlexTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppNavState {
    SPLASH,
    WELCOME,
    SETUP,
    MAIN
}

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // BLE & location permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request modern Android permissions
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

            // App Navigation State - if already registered, start at MAIN, otherwise SPLASH
            val isInitiallyRegistered = remember { UserSession.isRegistered(this@MainActivity) }
            var navState by remember { mutableStateOf(if (isInitiallyRegistered) AppNavState.MAIN else AppNavState.SPLASH) }

            // User Identity State - loaded from persistent session (empty if new user, NO hardcoded mock data)
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

            // Real data collections loaded from backend (NO hardcoded fake peers or requests)
            val nearbyPeers = remember { mutableStateListOf<NearbyPeerUiModel>() }
            val requestedPeerIds = remember { mutableStateListOf<String>() }
            val incomingRequests = remember { mutableStateListOf<ConnectionRequestIncoming>() }
            val mutualConnections = remember { mutableStateListOf<MutualConnection>() }

            val apiClient = remember { SyncApiClient() }
            val coroutineScope = rememberCoroutineScope()

            // Synchronize with backend API
            fun fetchBackendData() {
                if (currentUserId.isBlank()) return
                coroutineScope.launch {
                    // 1. Fetch nearby peers from backend
                    val peersRes = apiClient.getNearbyPeers(currentUserId)
                    if (peersRes.isSuccess) {
                        val peers = peersRes.getOrNull() ?: emptyList()
                        nearbyPeers.clear()
                        nearbyPeers.addAll(peers)
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

            // Periodic sync every 15 seconds when active
            LaunchedEffect(navState, currentUserId) {
                if (navState == AppNavState.MAIN && currentUserId.isNotBlank()) {
                    while (true) {
                        fetchBackendData()
                        delay(15_000)
                    }
                }
            }

            // Thermal & Battery monitoring
            DisposableEffect(Unit) {
                val thermalController = ThermalAndBatteryController(this@MainActivity) { _, shouldPause ->
                    if (shouldPause) {
                        powerMode = RadarPowerMode.OFF
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
                    AppNavState.SETUP -> navState = AppNavState.WELCOME
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
                                onGetStarted = { navState = AppNavState.SETUP }
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
                                                }
                                            )
                                        }

                                        AuraTab.DISCOVERED -> {
                                            DiscoveredScreen(
                                                nearbyPeers = nearbyPeers,
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
                                                encountersCount = nearbyPeers.size,
                                                connectedCount = mutualConnections.size,
                                                powerMode = powerMode,
                                                onPowerModeChanged = { newMode ->
                                                    powerMode = newMode
                                                    if (currentUserId.isNotBlank()) {
                                                        coroutineScope.launch {
                                                            apiClient.toggleDiscovery(currentUserId, newMode != RadarPowerMode.OFF)
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
                                                    navState = AppNavState.SPLASH
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
