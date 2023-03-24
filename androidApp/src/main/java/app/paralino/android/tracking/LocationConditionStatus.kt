package app.paralino.android.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.paralino.core.provider.LocationConditionStatus

fun LocationConditionStatus.Companion.isLocationOn(context: Context): Boolean {
    return ContextCompat.getSystemService(context, LocationManager::class.java)
        ?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
}

fun LocationConditionStatus.Companion.hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun LocationConditionStatus.Companion.hasNotificationsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

fun LocationConditionStatus.Companion.hasBackgroundLocationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        return true
    }
}

fun LocationConditionStatus.Companion.getLocationStatus(context: Context): LocationConditionStatus {
    return LocationConditionStatus(
        isGpsOn = isLocationOn(context),
        isPermissionAllowed = hasLocationPermission(context),
        isBackgroundPermissionAllowed = hasBackgroundLocationPermission(context),
        isNotificationPermissionAllowed = hasNotificationsPermission(context)
    )
}