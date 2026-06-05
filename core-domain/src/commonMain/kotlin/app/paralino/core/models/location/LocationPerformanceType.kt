package app.paralino.core.models.location

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class LocationPerformanceType {
    HIGH_BATTERY_SAVER,
    BATTERY_SAVER,
    BALANCED,
    MEDIUM_HIGH,
    HIGH,
    HIGHER_FOCUS,
    VERY_HIGH,
    MANUAL_BOOST;

    val defaultDuration: Duration
        get() = when (this) {
            HIGH_BATTERY_SAVER -> 0.seconds
            BATTERY_SAVER -> 0.seconds
            BALANCED -> 0.seconds
            MEDIUM_HIGH -> 30.seconds
            HIGH -> 30.seconds
            HIGHER_FOCUS -> 45.seconds
            VERY_HIGH -> 60.seconds
            MANUAL_BOOST -> 120.seconds
        }

    val stepDown: LocationPerformanceType
        get() = when (this) {
            HIGH_BATTERY_SAVER -> HIGH_BATTERY_SAVER
            BATTERY_SAVER -> BATTERY_SAVER
            BALANCED -> BALANCED
            MEDIUM_HIGH -> BALANCED
            HIGH -> BALANCED
            HIGHER_FOCUS -> BALANCED
            VERY_HIGH -> HIGH
            MANUAL_BOOST -> BALANCED
        }
}

data class LocationPerformanceRequest(
    val type: LocationPerformanceType,
    val duration: Duration
)