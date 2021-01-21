package com.zbapp.locationtrack.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow


class LocationInitHandler(
    private val context: Context
) {

    private val fusedLocationClient =  LocationServices.getFusedLocationProviderClient(context)

    private var locationRequest: LocationRequest = LocationRequest().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private var currentLocation = MutableStateFlow<Location?>(null)


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if(LocationConditionStatus.isLocationStatusOk(context)) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }


    private fun stopLocationUpdates() {
        currentLocation.value = null
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var locationCallback = object: LocationCallback() {
        override fun onLocationResult(location: LocationResult?) {
            location?.lastLocation?.let {
                currentLocation.value = it
            }
        }
    }


}