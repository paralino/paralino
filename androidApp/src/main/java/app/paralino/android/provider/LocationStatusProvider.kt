package app.paralino.android.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import app.paralino.core.provider.ILocationStatusProvider
import app.paralino.core.provider.LocationConditionStatus

class LocationStatusProvider(
    private val context: Context
) : ILocationStatusProvider() {

    init {
        refreshLocationStatus()
    }

    override fun createCurrentStatus(): LocationConditionStatus {
        return LocationConditionStatus.getLocationStatus(context)
    }
}

fun LocationConditionStatus.Companion.isLocationOn(context: Context): Boolean {
    return ContextCompat.getSystemService(context, LocationManager::class.java)
        ?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
}

fun LocationConditionStatus.Companion.hasCoarseLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun LocationConditionStatus.Companion.hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun LocationConditionStatus.Companion.hasBackgroundLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun LocationConditionStatus.Companion.hasUnrestrictedBackgroundAccess(context: Context): Boolean {
    return (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .isIgnoringBatteryOptimizations(context.packageName)
}

fun LocationConditionStatus.Companion.getLocationStatus(context: Context): LocationConditionStatus {
    return LocationConditionStatus(
        isGpsOn = isLocationOn(context),
        isFinePermissionAllowed = hasFineLocationPermission(context),
        isCoarsePermissionAllowed = hasCoarseLocationPermission(context),
        isBackgroundPermissionAllowed = hasBackgroundLocationPermission(context),
        isUnrestrictedBackgroundPermissionAllowed = hasUnrestrictedBackgroundAccess(context)
    )
}