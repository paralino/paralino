package app.paralino.android.tracking

import Logger
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import app.paralino.core.models.Payload
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.utils.LocationConditionStatus
import app.paralino.core.utils.LocationStatusException
import app.paralino.core.utils.extensions.randomString
import app.paralino.android.utils.extensions.batterLevel
import app.paralino.android.utils.extensions.millisToDateTimeUTC
import app.paralino.android.utils.extensions.toLocalString
import kotlinx.coroutines.flow.MutableStateFlow


@SuppressLint("MissingPermission")
class LocationInitHandler(
    private val context: Context
) {

    private val fusedLocationClient =  LocationServices.getFusedLocationProviderClient(context)

    private var locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 6_000
        fastestInterval = 3_000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    val lastLocation = MutableStateFlow<LocationDataModel?>(null)
    val currentLocation = MutableStateFlow<Payload<LocationDataModel>>(Payload.Error(LocationStoppedException()))

    val isTracking: Boolean get() = currentLocation.value is Payload.Success

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


    fun startLocationUpdates(looper: Looper = Looper.getMainLooper()) {
        Logger.v("start location updates")
        val status = LocationConditionStatus.getLocationStatus(context)
        if(status.isOK()) {
            sessionId = randomString(20)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                looper
            ).addOnFailureListener {
                currentLocation.value = Payload.Error(it)
            }
        } else {
            currentLocation.value = Payload.Error(LocationStatusException(status))
        }
    }


    fun stopLocationUpdates() {
        Logger.v("stopping all location updates")
        currentLocation.value = Payload.Error(LocationStoppedException())
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object: LocationCallback() {
        override fun onLocationResult(location: LocationResult) {
            location.lastLocation.let {
                val value = LocationDataModel(
                    lat = it.latitude,
                    lng = it.longitude,
                    accuracy = it.accuracy,
                    timeUTC = it.time.millisToDateTimeUTC().toLocalString(),
                    batteryLevel = context.batterLevel,
                    speed = if(it.hasSpeed()) it.speed else null,
                    altitude = if(it.hasAltitude()) it.altitude.toFloat() else null
                )
                lastLocation.value = value
                currentLocation.value = Payload.Success(value)
            }
        }
    }

}

class LocationStoppedException(): Exception("Location tracking/sharing stopped")