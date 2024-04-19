package app.paralino.android.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import app.paralino.android.utils.extensions.millisToDateTimeUTC
import app.paralino.android.utils.extensions.toLocalString
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.provider.LocationConditionStatus
import app.paralino.core.provider.LocationStatusException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn


@SuppressLint("MissingPermission")
class LocationInitHandler constructor(
    private val context: Context,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val scope: ICoroutineScopeProvider,
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationTrackingFlag = MutableStateFlow(false)
    val currentLocation = MutableStateFlow<Payload<LocationDataModel>>(
        Payload.Error(LocationStoppedException())
    )

    val isTracking: Boolean get() = currentLocation.value is Payload.Success

    init {
        combine(
            locationTrackingFlag,
            locationPerformanceManager.locationPerformanceType
        ) { tracking, performanceType ->
            logg.tag("location_request").v("location request: ${performanceType.current}")
            if (tracking) {
                logg.tag("location_request").v("location request OK: ${performanceType.current.name}")
                val status = LocationConditionStatus.getLocationStatus(context)
                if (status.isOK()) {

                    logg.tag("location_request").v("new request: $performanceType")
                    val locationRequest = LocationPerformanceRequests.getRequest(performanceType.current)
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

    fun startLocationUpdates() {
        logg.tag("location_request").v("start location updates")
        locationTrackingFlag.value = true
    }

    fun stopLocationUpdates() {
        logg.tag("location_request").v("stopping all location updates")
        locationTrackingFlag.value = false
        currentLocation.value = Payload.Error(LocationStoppedException())
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object : LocationCallback() {
        override fun onLocationResult(location: LocationResult) {
            logg.tag("location_request").v("Got location update from system")
            location.lastLocation?.let {
                val value = LocationDataModel(
                    lat = it.latitude,
                    lng = it.longitude,
                    accuracy = it.accuracy,
                    timeUTC = it.time.millisToDateTimeUTC().toLocalString(),
                    speed = if (it.hasSpeed()) it.speed else null,
                    altitude = if (it.hasAltitude()) it.altitude.toFloat() else null
                )
                currentLocation.value = Payload.Success(value)
            }
        }
    }

}

class LocationStoppedException() : Exception("Location tracking/sharing stopped")