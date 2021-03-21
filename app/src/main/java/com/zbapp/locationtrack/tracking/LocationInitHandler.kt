package com.zbapp.locationtrack.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.zbapp.location.models.Payload
import com.zbapp.location.models.user.UserMe
import com.zbapp.locationtrack.data.config.randomString
import com.zbapp.locationtrack.data.models.location.LocationEntity
import com.zbapp.locationtrack.utils.extensions.batterLevel
import kotlinx.coroutines.flow.*
import java.lang.Exception


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

    val lastLocation = MutableStateFlow<LocationEntity?>(null)
    val currentLocation = MutableStateFlow<Payload<LocationEntity>>(Payload.Error(LocationStoppedException()))

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
        currentLocation.value = Payload.Error(LocationStoppedException())
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object: LocationCallback() {
        override fun onLocationResult(location: LocationResult?) {
            location?.lastLocation?.let {
                val value = LocationEntity.fromSystem(
                    it, sessionId, UserMe("user_me", "ME", "test", null, ""), context.batterLevel
                )
                lastLocation.value = value
                currentLocation.value = Payload.Success(value)
            }
        }
    }

}

class LocationStoppedException(): Exception("Location tracking/sharing stopped")