package com.zbapp.locationtrack.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.zbapp.location.models.Payload
import com.zbapp.locationtrack.data.config.randomString
import com.zbapp.locationtrack.data.models.location.LocationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.lang.Exception
import kotlin.random.Random


@SuppressLint("MissingPermission")
class LocationInitHandler(
    private val context: Context
) {

    private val fusedLocationClient =  LocationServices.getFusedLocationProviderClient(context)

    private var locationRequest: LocationRequest = LocationRequest().apply {
        interval = 8_000
        fastestInterval = 1_000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    val lastLocation = MutableStateFlow<LocationModel?>(null)
    val currentLocation = MutableStateFlow<Payload<LocationModel>>(Payload.Error(LocationStoppedException()))

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


    fun startLocationUpdates() {
        val status = LocationConditionStatus.getLocationStatus(context)
        if(status == LocationConditionStatus.OK) {
            sessionId = randomString(20)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            ).addOnFailureListener {
                currentLocation.value = Payload.Error(it)
            }
        } else {
            currentLocation.value = Payload.Error(LocationStatusException(status))
        }
    }


    fun stopLocationUpdates() {
        currentLocation.value = Payload.Error(LocationStoppedException())
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object: LocationCallback() {
        override fun onLocationResult(location: LocationResult?) {
            location?.lastLocation?.let {
                val value = LocationModel.fromSystem(
                    it, sessionId, "user_test", null
                )
                lastLocation.value = value
                currentLocation.value = Payload.Success(value)
            }
        }
    }

}

class LocationStoppedException(): Exception("Location tracking/sharing stopped")