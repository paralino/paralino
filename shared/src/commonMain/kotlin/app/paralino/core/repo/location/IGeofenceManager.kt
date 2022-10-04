package app.paralino.core.repo.location

import app.paralino.core.models.places.GroupPlace

interface IGeofenceManager {

    fun registerGeofences(places: List<GroupPlace>)
    fun deleteGeofences(placesIds: List<String>)

}