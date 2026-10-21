package com.ghostmesh.app.proximity

import android.os.SystemClock
import java.util.ArrayDeque

enum class ProximityTier(val label: String, val canTriggerPushAlert: Boolean) {
    VERY_CLOSE("Very Close", true),        // High signal confidence (Immediate room/table)
    NEARBY("Nearby", true),                // Medium signal confidence (Same hall/zone)
    EXTENDED("Extended Range", false)      // Extended signal confidence (Silent discovery, no alert vibration)
}

typealias ConfidenceCategory = ProximityTier

data class FilteredBleObservation(
    val deviceToken: String,
    val smoothedRssi: Int,
    val proximityTier: ProximityTier,
    val isConfident: Boolean,
    val lastSeenElapsedRealtime: Long // Monotonic clock: Immune to network time jumps
)

/**
 * Production-Calibrated ProximityEvaluator:
 * - Uses SystemClock.elapsedRealtime() for monotonic, jump-immune absence timing.
 * - Rolling median filtering over consecutive observations.
 * - Hysteresis entry gate (requires sustained observations).
 * - Departure is determined strictly by 15-second time absence, not momentary RSSI dips.
 */
class ProximityEvaluator(
    private val windowSize: Int = 6,
    private val entryThresholdDbm: Int = -95,
    private val minSightingsForExtended: Int = 2,
    private val minSightingsForClose: Int = 1
) {
    private val rssiHistory = mutableMapOf<String, ArrayDeque<Int>>()
    private val activeEncounters = mutableSetOf<String>()

    fun recordObservation(token: String, rawRssi: Int): FilteredBleObservation? {
        val nowElapsed = SystemClock.elapsedRealtime()
        val history = rssiHistory.getOrPut(token) { ArrayDeque(windowSize) }

        if (history.size >= windowSize) {
            history.removeFirst()
        }
        history.addLast(rawRssi)

        // Rolling Median Filter for stable radio observation
        val sortedReadings = history.sorted()
        val medianRssi = sortedReadings[sortedReadings.size / 2]

        val isCurrentlyActive = activeEncounters.contains(token)

        // Require minimum valid sightings before activating an encounter
        val meetsEntryCriteria = isCurrentlyActive || (
            medianRssi >= entryThresholdDbm && history.size >= minSightingsForExtended
        )

        if (meetsEntryCriteria) {
            activeEncounters.add(token)

            // Map to Signal-Confidence Categories
            val tier = when {
                medianRssi >= -70 -> ProximityTier.VERY_CLOSE
                medianRssi >= -82 -> ProximityTier.NEARBY
                else -> ProximityTier.EXTENDED
            }

            return FilteredBleObservation(
                deviceToken = token,
                smoothedRssi = medianRssi,
                proximityTier = tier,
                isConfident = history.size >= (if (tier == ProximityTier.EXTENDED) minSightingsForExtended else minSightingsForClose),
                lastSeenElapsedRealtime = nowElapsed
            )
        }
        return null
    }

    /**
     * Departure logic: Uses monotonic SystemClock.elapsedRealtime() to prevent
     * false departures caused by network time syncs or manual clock adjustments.
     */
    fun isAbsent(lastSeenElapsedRealtime: Long, absenceTimeoutMs: Long = 15_000L): Boolean {
        return (SystemClock.elapsedRealtime() - lastSeenElapsedRealtime) > absenceTimeoutMs
    }

    fun removeToken(token: String) {
        rssiHistory.remove(token)
        activeEncounters.remove(token)
    }
}
