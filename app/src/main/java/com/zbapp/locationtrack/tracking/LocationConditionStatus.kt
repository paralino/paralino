package com.zbapp.locationtrack.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.lang.Exception
import java.lang.IllegalStateException

data class LocationConditionStatus constructor(
    val isGpsOn: Boolean,
    val isPermissionAllowed: Boolean
) {

    fun isOK() = isGpsOn && isPermissionAllowed


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

        fun isOk(context: Context): Boolean {
            return getLocationStatus(context).isOK()
        }

        fun getLocationStatus(context: Context): LocationConditionStatus {
            return LocationConditionStatus(
                isGpsOn = isLocationOn(context),
                isPermissionAllowed = hasLocationPermission(context)
            )
        }
    }
}

class LocationStatusException(val status: LocationConditionStatus): IllegalStateException(status.toString())