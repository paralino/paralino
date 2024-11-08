package app.paralino.android.tracking.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.paralino.core.logg
import app.paralino.core.repo.places.PlaceActivityTriggerType
import app.paralino.core.repo.places.PlacesRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val receiver: GeofenceReceiver by lazy { GeofenceReceiver() }

    override fun onReceive(context: Context?, intent: Intent?) {

        intent ?: return
        context ?: return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            logg.v("place_activity_geofence", "Geofence event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            logg.i("place_activity_geofence", "Geofence error: $errorMessage")
            return
        }

        when (val transition = geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                geofencingEvent.triggeringGeofences?.let { receiver.notifyPlaceEnter(it.map { it.requestId }) }
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                geofencingEvent.triggeringGeofences?.let { receiver.notifyPlaceExit(it.map { it.requestId }) }
            }

            else -> {
                logg.i("place_activity_geofence", "Invalid geofence type: $transition")
            }
        }
    }
}

class GeofenceReceiver() : KoinComponent {
    private val geofenceProcessor: PlacesRepository by inject()

    fun notifyPlaceEnter(placeIds: List<String>) {
        geofenceProcessor.placesEnter(
            placesIds = placeIds,
            trigger = PlaceActivityTriggerType.GOEFENCE
        )
    }

    fun notifyPlaceExit(placeIds: List<String>) {
        geofenceProcessor.placesExit(
            placesIds = placeIds,
            trigger = PlaceActivityTriggerType.GOEFENCE
        )
    }
}