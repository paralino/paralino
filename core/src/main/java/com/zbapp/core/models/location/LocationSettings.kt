package com.zbapp.core.models.location

data class LocationSettings(
    val minAccuracyMeters: Int,
    val minUpdateTimeSec: Int,
    val performance: LocationPerformanceType,
    val motionDetection: Boolean
)

enum class LocationPerformanceType {
    HIGH, BALANCED, LOW
}
