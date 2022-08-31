package app.paralino.android.tracking

import Logger
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.android.utils.extensions.millisToDateTimeUTC
import app.paralino.android.utils.extensions.toLocalString
import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.utils.LocationConditionStatus
import app.paralino.core.utils.LocationStatusException
import app.paralino.core.utils.extensions.randomString
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlin.time.Duration.Companion.minutes


@SuppressLint("MissingPermission")
class LocationInitHandler constructor(
    private val context: Context,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val scope: ICoroutineScopeProvider,
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequestLow: LocationRequest get() = LocationRequest.create().apply {
        interval = 15 * 60 * 1000
        fastestInterval = 4 * 60 * 1000
//        smallestDisplacement = 200f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationRequestBalanced: LocationRequest get() = LocationRequest.create().apply {
        interval = 10 * 60 * 1000L
        fastestInterval = 2 * 60 * 1000L
//        smallestDisplacement = 50f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationRequestHigh: LocationRequest get() = LocationRequest.create().apply {
        interval = 3_000
        fastestInterval = 2_000
//        smallestDisplacement = 10f
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationTrackingFlag = MutableStateFlow(false)
    val lastLocation = MutableStateFlow<LocationDataModel?>(null)
    val currentLocation = MutableStateFlow<Payload<LocationDataModel>>(
        Payload.Error(LocationStoppedException())
    )

    val isTracking: Boolean get() = currentLocation.value is Payload.Success

    init {
        combine(
            locationTrackingFlag,
            locationPerformanceManager.locationPerformanceType
        ) { tracking, performanceType ->
            logg.tag("location_request").v("location request: ${performanceType.name}")
            if (tracking) {
                logg.tag("location_request").v("location request OK: ${performanceType.name}")
                val status = LocationConditionStatus.getLocationStatus(context)
                if (status.isOK()) {
                    sessionId = randomString(20)

                    logg.tag("location_request")
                        .v("new request: ${performanceType.name}")
                    val locationRequest = when (performanceType) {
                        LocationPerformanceType.HIGH -> locationRequestHigh
                        LocationPerformanceType.BALANCED -> locationRequestBalanced
                        LocationPerformanceType.LOW -> locationRequestLow
                    }

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    ).addOnFailureListener {
                        logg.tag("location_request").v("request error", it)
                        currentLocation.value = Payload.Error(it)
                    }

                } else {
                    logg.tag("location_request").v("status not ok: $status")
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    currentLocation.value = Payload.Error(LocationStatusException(status))
                }
            }
        }
            .launchIn(scope.defaultIOScope)
    }

//    val locationFlow = fusedLocationClient.locationFlow()
//        .stateIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), Payload.Loading)
//
//    fun FusedLocationProviderClient.locationFlow(): Flow<Payload<LocationModel>>{
//        return callbackFlow {
//
//            if(!LocationConditionStatus.isLocationStatusOk(context)) {
//                val error = Throwable("Location status != OK")
//                this.offer(Payload.Error(error))
//                close(error)
//                return@callbackFlow
//            }
//
//            val callback = object : LocationCallback() {
//                override fun onLocationResult(result: LocationResult?) {
//                    result ?: return
//                    for (location in result.locations) {
//                        try {
//                            offer(Payload.Success(location))
//                        } catch (e: Exception) {
//                        }
//                    }
//                }
//            }
//
//            requestLocationUpdates(
//                locationRequest,
//                callback,
//                Looper.getMainLooper()
//            ).addOnFailureListener { close(it) }
//
//            awaitClose {
//                this.offer(Payload.Error(Throwable("canceled")))
//                removeLocationUpdates(callback)
//            }
//
//        }
//    }

    private var sessionId: String = ""


    fun startLocationUpdates() {
        Logger.tag("location_request").v("start location updates")
        locationTrackingFlag.value = true
    }

    fun stopLocationUpdates() {
        Logger.tag("location_request").v("stopping all location updates")
        locationTrackingFlag.value = false
        currentLocation.value = Payload.Error(LocationStoppedException())
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(location: LocationResult) {
            logg.tag("location_request").v("got location update")
            location.lastLocation.let {
                val value = LocationDataModel(
                    lat = it.latitude,
                    lng = it.longitude,
                    accuracy = it.accuracy,
                    timeUTC = it.time.millisToDateTimeUTC().toLocalString(),
                    batteryLevel = context.batterLevel,
                    speed = if (it.hasSpeed()) it.speed else null,
                    altitude = if (it.hasAltitude()) it.altitude.toFloat() else null
                )
                lastLocation.value = value
                currentLocation.value = Payload.Success(value)
            }
        }
    }

}

class LocationStoppedException() : Exception("Location tracking/sharing stopped")