package app.paralino.android.tracking.geofence

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresPermission
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.repo.location.IGeofenceManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlin.time.Duration.Companion.hours

class GeofenceManager(
    private val application: Application
) : IGeofenceManager {

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(application, GeofenceBroadcastReceiver::class.java)

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            flags = flags or PendingIntent.FLAG_MUTABLE

        PendingIntent.getBroadcast(
            application,
            0,
            intent,
            flags
        )
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    override fun registerGeofences(places: List<GroupPlaceGeofenceData>) {
        val geofencingClient = LocationServices.getGeofencingClient(application)

        val geofences = places.map { place ->
            createGeofence(
                id = place.placeId,
                lat = place.lat,
                lng = place.lng,
                radiusMeters = place.radiusMeters
            )
        }

        val request = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofences)
        }.build()

        geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
            logg.tag("place_activity_geofence").v("Previous geofences removed, creating new ones")
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnFailureListener {
                    logg.tag("place_activity_geofence").w("Error creating geofence", it)
                }
                .addOnSuccessListener {
                    logg.tag("place_activity_geofence").d("Successfully created geofences")
                }
        }
    }

    private fun createGeofence(id: String, lat: Double, lng: Double, radiusMeters: Int): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                lat,
                lng,
                radiusMeters.toFloat()
            )
            .setExpirationDuration(24L.hours.inWholeMilliseconds)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
    }

    override fun distanceBetweenPoints(
        data: LocationDataModel,
        geofence: GroupPlaceGeofenceData
    ): Float {
        return distanceBetweenPoints(
            lat1 = data.lat,
            lng1 = data.lng,
            lat2 = geofence.lat,
            lng2 = geofence.lng
        )
    }

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

    override fun deleteAllGeofences() {
        val geofencingClient = LocationServices.getGeofencingClient(application)
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}