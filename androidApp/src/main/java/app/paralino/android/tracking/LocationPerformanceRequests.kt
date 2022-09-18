package app.paralino.android.tracking

import app.paralino.core.models.location.LocationPerformanceType
import com.google.android.gms.location.LocationRequest

object LocationPerformanceRequests {

    fun getRequest(type: LocationPerformanceType): LocationRequest {
        return when(type) {
            LocationPerformanceType.HIGH -> locationRequestHigh
            LocationPerformanceType.MEDIUM_HIGH -> locationRequestMediumHigh
            LocationPerformanceType.BALANCED -> locationRequestBalanced
            LocationPerformanceType.DEFAULT -> locationRequestLow
        }
    }

    private val locationRequestLow: LocationRequest
        get() = LocationRequest.create().apply {
        interval = 30 * 60 * 1000
        fastestInterval = 10 * 60 * 1000
//        smallestDisplacement = 200f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationRequestBalanced: LocationRequest
        get() = LocationRequest.create().apply {
        interval = 15 * 60 * 1000L
        fastestInterval = 2 * 60 * 1000L
//        smallestDisplacement = 50f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationRequestMediumHigh: LocationRequest
        get() = LocationRequest.create().apply {
        interval = 5 * 60 * 1000L
        fastestInterval = 10_000
//        smallestDisplacement = 10f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationRequestHigh: LocationRequest
        get() = LocationRequest.create().apply {
        interval = 3_000
        fastestInterval = 2_000
//        smallestDisplacement = 10f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

}