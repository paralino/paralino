package app.paralino.core.repo.location

import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.places.GroupPlaceGeofenceData

interface IGeofenceManager {
    fun registerGeofences(places: List<GroupPlaceGeofenceData>)
    fun distanceBetweenPoints(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float
    fun deleteAllGeofences()

    fun distanceBetweenPoints(data: LocationDataModel, geofence: GroupPlaceGeofenceData): Float {
        return distanceBetweenPoints(
            lat1 = data.lat,
            lng1 = data.lng,
            lat2 = geofence.lat,
            lng2 = geofence.lng
        )
    }
}