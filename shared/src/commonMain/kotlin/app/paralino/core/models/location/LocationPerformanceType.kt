package app.paralino.core.models.location

enum class LocationPerformanceType {
    HIGH_BATTERY_SAVER,
    BATTERY_SAVER,
    BALANCED,
    MEDIUM_HIGH,
    HIGH,
    VERY_HIGH,
    MANUAL_BOOST;

    val defaultDurationMillis: Long
        get() = when (this) {
            HIGH_BATTERY_SAVER -> 0
            BATTERY_SAVER -> 0
            BALANCED -> 0
            MEDIUM_HIGH -> 30_000
            HIGH -> 30_000
            VERY_HIGH -> 60_000
            MANUAL_BOOST -> 120_000
        }

    val stepDown: LocationPerformanceType
        get() = when (this) {
            HIGH_BATTERY_SAVER -> HIGH_BATTERY_SAVER
            BATTERY_SAVER -> BATTERY_SAVER
            BALANCED -> BALANCED
            MEDIUM_HIGH -> BALANCED
            HIGH -> BALANCED
            VERY_HIGH -> HIGH
            MANUAL_BOOST -> BALANCED
        }
}

data class LocationPerformanceRequest(
    val type: LocationPerformanceType,
    val durationMillis: Long
)