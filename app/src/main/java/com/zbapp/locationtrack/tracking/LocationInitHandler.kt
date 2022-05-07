package com.zbapp.locationtrack.tracking

import Logger
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.zbapp.core.models.Payload
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.utils.LocationConditionStatus
import com.zbapp.core.utils.LocationStatusException
import com.zbapp.core.utils.extensions.randomString
import com.zbapp.locationtrack.utils.extensions.batterLevel
import com.zbapp.locationtrack.utils.extensions.toDateTimeUTC
import com.zbapp.locationtrack.utils.extensions.toLocalString
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
            location?.lastLocation?.let {
                val value = LocationDataModel(
                    it.latitude, it.longitude, it.accuracy, it.time.toDateTimeUTC().toLocalString(), context.batterLevel
                )
                lastLocation.value = value
                currentLocation.value = Payload.Success(value)
            }
        }
    }

}

class LocationStoppedException(): Exception("Location tracking/sharing stopped")