package app.paralino.android.tracking.geofence

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import app.paralino.core.models.places.GroupPlace
import app.paralino.core.repo.location.IGeofenceManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(
    private val application: Application
): IGeofenceManager {

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(application, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            application,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    override fun registerGeofences(places: List<GroupPlace>) {
        val geofencingClient = LocationServices.getGeofencingClient(application)

        val geofences = places.map { place ->
            createGeofence(
                id = place.id,
                lat = place.data.lat,
                lng = place.data.lng,
                radiusMeters = place.data.radiusMeters
            )
        }

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    private fun createGeofence(id: String, lat: Double, lng: Double, radiusMeters: Int): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                lat,
                lng,
                radiusMeters.toFloat()
            )
            .setExpirationDuration(-1)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    override fun deleteGeofences(placesIds: List<String>) {
        val geofencingClient = LocationServices.getGeofencingClient(application)
        geofencingClient.removeGeofences(placesIds)
    }

}