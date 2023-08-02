package app.paralino.core.models.location

data class LocationSettings(
    val minAccuracyMeters: Int,
    val minUpdateTimeSec: Int,
    val performance: LocationPerformanceType,
    val motionDetection: Boolean
)

enum class LocationPerformanceType {
    BATTERY_SAVER,
    BALANCED,
    MEDIUM_HIGH,
    HIGH,
    VERY_HIGH
}