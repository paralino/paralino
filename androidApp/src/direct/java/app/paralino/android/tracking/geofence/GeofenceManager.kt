package app.paralino.android.tracking.geofence

import android.app.Application
import android.location.Location
import androidx.annotation.RequiresPermission
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.repo.location.IGeofenceManager

class GeofenceManager(
    private val application: Application
) : IGeofenceManager {

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    override fun registerGeofences(places: List<GroupPlaceGeofenceData>) {}
    override fun deleteAllGeofences() {}

    override fun distanceBetweenPoints(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result.first()
    }
}