package com.zbapp.locationtrack.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

enum class LocationConditionStatus {
    LOCATION_PERMISSION_DENIED,
    LOCATION_SERVICE_DISABLED,
    OK;


    companion object {
        fun isLocationOn(context: Context): Boolean {
            return ContextCompat.getSystemService(context, LocationManager::class.java)
                ?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }

        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun isLocationStatusOk(context: Context): Boolean {
            return getLocationStatus(context) == OK
        }

        fun getLocationStatus(context: Context): LocationConditionStatus {
            return if(!hasLocationPermission(context)) {
                LOCATION_PERMISSION_DENIED
            } else if(!isLocationOn(context)) {
                LOCATION_SERVICE_DISABLED
            } else {
                OK
            }
        }
    }
}