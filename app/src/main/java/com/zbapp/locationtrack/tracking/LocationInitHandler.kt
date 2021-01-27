package com.zbapp.locationtrack.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.zbapp.locationtrack.data.models.location.LocationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random


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
    private var sessionId: String = ""


    @SuppressLint("MissingPermission")
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