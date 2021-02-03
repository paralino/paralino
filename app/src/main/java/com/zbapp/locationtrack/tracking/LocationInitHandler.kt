package com.zbapp.locationtrack.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.zbapp.locationtrack.data.database.Payload
import com.zbapp.locationtrack.data.models.location.LocationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.random.Random


@SuppressLint("MissingPermission")
class LocationInitHandler(
    private val context: Context
) {

    private val fusedLocationClient =  LocationServices.getFusedLocationProviderClient(context)

    private var locationRequest: LocationRequest = LocationRequest().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    val currentLocation = MutableStateFlow<LocationModel?>(null)

//    val isTracking: Boolean get() = locationFlow.value is Payload.Success
//
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
        if(LocationConditionStatus.isLocationStatusOk(context)) {
            sessionId = Random.nextInt(1000, 1000000).toString()
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }


    fun stopLocationUpdates() {
        currentLocation.value = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object: LocationCallback() {
        override fun onLocationResult(location: LocationResult?) {
            location?.lastLocation?.let {
                currentLocation.value = LocationModel.fromSystem(
                    it, sessionId, "user_test", null
                )
            }
        }
    }


}