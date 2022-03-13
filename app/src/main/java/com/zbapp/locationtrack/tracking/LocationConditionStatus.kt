package com.zbapp.locationtrack.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.zbapp.locationtrack.ui.shared.list.ErrorListItemType
import java.lang.Exception
import java.lang.IllegalStateException

data class LocationConditionStatus constructor(
    val isGpsOn: Boolean,
    val isPermissionAllowed: Boolean,
    val isBackgroundPermissionAllowed: Boolean
) {

    fun isOK(forAlwaysOnAccess: Boolean = false): Boolean {
        return isGpsOn && isPermissionAllowed && (if(forAlwaysOnAccess) isBackgroundPermissionAllowed else true)
    }

    fun getPossibleErrors(forBackground: Boolean = true): MutableList<ErrorListItemType> {
        val list = mutableListOf<ErrorListItemType>()
        if(!isGpsOn) list.add(ErrorListItemType.GPS)
        if(!isPermissionAllowed) list.add(ErrorListItemType.PERMISSION)
        if(!isBackgroundPermissionAllowed && forBackground) list.add(ErrorListItemType.PERMISSION_BACKGROUND_LOCATION)
        return list
    }

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

        fun hasBackgroundLocationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                return true
            }
        }

        fun getLocationStatus(context: Context): LocationConditionStatus {
            return LocationConditionStatus(
                isGpsOn = isLocationOn(context),
                isPermissionAllowed = hasLocationPermission(context),
                isBackgroundPermissionAllowed = hasBackgroundLocationPermission(context)
            )
        }
    }
}

class LocationStatusException(val status: LocationConditionStatus): IllegalStateException(status.toString())