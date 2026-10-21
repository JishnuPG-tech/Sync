package com.ghostmesh.app.data.repository

import com.ghostmesh.app.data.model.NearbyPeerUiModel
import com.ghostmesh.app.proximity.FilteredBleObservation
import com.ghostmesh.app.proximity.ProximityEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class ActiveEncounter(
    val token: String,
    val observation: FilteredBleObservation,
    val profile: NearbyPeerUiModel?
)

/**
 * EncounterRepository with In-Memory Profile Caching:
 * - Caches resolved profiles by token to prevent redundant network lookups.
 * - Deduplicates continuous BLE observations.
 * - Orders nearby peers by confidence tier and freshness.
 * - Prunes departed encounters using monotonic absence timeouts.
 * - CRITICAL PRIVACY: Instagram username is NEVER exposed or cached here!
 */
class EncounterRepository(
    private val proximityEvaluator: ProximityEvaluator
) {
    private val encountersMap = ConcurrentHashMap<String, ActiveEncounter>()
    
    // In-memory cache of already resolved profiles: Prevents repeated HTTP lookups!
    private val resolvedProfileCache = ConcurrentHashMap<String, NearbyPeerUiModel>()

    private val _nearbyPeersFlow = MutableStateFlow<List<NearbyPeerUiModel>>(emptyList())
    val nearbyPeersFlow: StateFlow<List<NearbyPeerUiModel>> = _nearbyPeersFlow.asStateFlow()

    fun getCachedProfile(token: String): NearbyPeerUiModel? = resolvedProfileCache[token]

    fun onBleObservation(observation: FilteredBleObservation, resolvedProfile: NearbyPeerUiModel) {
        // Cache the profile for future sightings in this session
        resolvedProfileCache[observation.deviceToken] = resolvedProfile

        encountersMap[observation.deviceToken] = ActiveEncounter(
            token = observation.deviceToken,
            observation = observation,
            profile = resolvedProfile.copy(
                confidenceTier = observation.proximityTier,
                lastSeenElapsedMs = observation.lastSeenElapsedRealtime
            )
        )
        refreshList()
    }

    /**
     * Called periodically (e.g. every 3 seconds) to remove departed devices.
     * Uses 20s for active foreground scanning, or 35s when screen is off to accommodate
     * Android's background BLE scan throttling.
     */
    fun pruneStaleEncounters(timeoutMs: Long = 20_000L) {
        var changed = false
        val iterator = encountersMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (proximityEvaluator.isAbsent(entry.value.observation.lastSeenElapsedRealtime, timeoutMs)) {
                proximityEvaluator.removeToken(entry.key)
                iterator.remove()
                changed = true
            }
        }
        if (changed) {
            refreshList()
        }
    }

    private fun refreshList() {
        val list = encountersMap.values
            .mapNotNull { it.profile }
            .sortedWith(
                compareBy<NearbyPeerUiModel> { it.confidenceTier.ordinal }
                    .thenByDescending { it.lastSeenElapsedMs }
            )
        _nearbyPeersFlow.value = list
    }

    fun clearAll() {
        encountersMap.clear()
        resolvedProfileCache.clear()
        _nearbyPeersFlow.value = emptyList()
    }
}
