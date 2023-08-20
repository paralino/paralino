package app.paralino.android.tracking

import app.paralino.core.models.location.LocationPerformanceType
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

object LocationPerformanceRequests {

    fun getRequest(type: LocationPerformanceType): LocationRequest {
        return when (type) {
            LocationPerformanceType.HIGH,
            LocationPerformanceType.VERY_HIGH -> locationRequestHigh
            LocationPerformanceType.MEDIUM_HIGH -> locationRequestMediumHigh
            LocationPerformanceType.BALANCED -> locationRequestBalanced
            LocationPerformanceType.BATTERY_SAVER -> locationRequestLow
        }
    }

    private val locationRequestLow: LocationRequest
        get() = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30 * 60 * 1000L)
            .setMinUpdateIntervalMillis(10 * 60 * 1000L)
            .build()

    private val locationRequestBalanced: LocationRequest
        get() = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15 * 60 * 1000L)
            .setMinUpdateIntervalMillis(2 * 60 * 1000L)
            .build()

    private val locationRequestMediumHigh: LocationRequest
        get() = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 60 * 1000L)
            .setMinUpdateIntervalMillis(10_000L)
            .build()

    private val locationRequestHigh: LocationRequest
        get() = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

}