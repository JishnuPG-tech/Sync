package com.spott.app.proximity

import android.os.SystemClock
import java.util.ArrayDeque

enum class ProximityTier(val label: String, val canTriggerPushAlert: Boolean) {
    VERY_CLOSE("Very Close", true),        // High signal confidence (Immediate room/table: 0-3m)
    NEARBY("Nearby", true),                // Medium signal confidence (Same hall/zone: 3-8m)
    EXTENDED("Extended Range", false)      // Extended signal confidence (8-25m)
}

typealias ConfidenceCategory = ProximityTier

data class FilteredBleObservation(
    val deviceToken: String,
    val smoothedRssi: Int,
    val proximityTier: ProximityTier,
    val isConfident: Boolean,
    val lastSeenElapsedRealtime: Long, // Monotonic clock: Immune to network time jumps
    val broadcastDisplayName: String? = null // Instant peer identity from BLE over-the-air packet
)

/**
 * Production-Calibrated ProximityEvaluator:
 * - Instant sighting: 1 valid packet immediately surfaces peer on radar (zero lag).
 * - Rolling median filtering over consecutive observations for rock-solid RSSI smoothing.
 * - Monotonic elapsedRealtime for departure and absence checks.
 */
class ProximityEvaluator(
    private val windowSize: Int = 3,
    private val entryThresholdDbm: Int = -105,
    private val minSightingsForExtended: Int = 1,
    private val minSightingsForClose: Int = 1
) {
    private val rssiHistory = mutableMapOf<String, ArrayDeque<Int>>()
    private val activeEncounters = mutableSetOf<String>()

    fun recordObservation(token: String, rawRssi: Int, displayName: String? = null): FilteredBleObservation? {
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

        val meetsEntryCriteria = isCurrentlyActive || (
            medianRssi >= entryThresholdDbm && history.size >= minSightingsForExtended
        )

        if (meetsEntryCriteria) {
            activeEncounters.add(token)

            // Map to Signal-Confidence Categories
            val tier = when {
                medianRssi >= -72 -> ProximityTier.VERY_CLOSE
                medianRssi >= -85 -> ProximityTier.NEARBY
                else -> ProximityTier.EXTENDED
            }

            return FilteredBleObservation(
                deviceToken = token,
                smoothedRssi = medianRssi,
                proximityTier = tier,
                isConfident = true,
                lastSeenElapsedRealtime = nowElapsed,
                broadcastDisplayName = displayName
            )
        }
        return null
    }

    /**
     * Departure logic: Uses monotonic SystemClock.elapsedRealtime() to prevent
     * false departures caused by network time syncs or manual clock adjustments.
     */
    fun isAbsent(lastSeenElapsedRealtime: Long, absenceTimeoutMs: Long = 30_000L): Boolean {
        return (SystemClock.elapsedRealtime() - lastSeenElapsedRealtime) > absenceTimeoutMs
    }

    fun removeToken(token: String) {
        rssiHistory.remove(token)
        activeEncounters.remove(token)
    }
}
