package app.paralino.android.tracking.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.paralino.core.logg
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.places.PlacesRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val receiver: GeofenceReceiver by lazy { GeofenceReceiver() }

    override fun onReceive(context: Context?, intent: Intent?) {

        intent ?: return
        context ?: return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if(geofencingEvent == null) {
            logg.v("geofences", "Geofence event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            logg.v("geofences", "Geofence error: $errorMessage")
            return
        }

        when (val transition = geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                logg.v("geofences", "User entered geofence")
                geofencingEvent.triggeringGeofences?.let { receiver.notifyPlaceEnter(it.map { it.requestId }) }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                logg.v("geofences", "User exited geofence")
                geofencingEvent.triggeringGeofences?.let { receiver.notifyPlaceExit(it.map { it.requestId }) }
            }
            else -> {
                logg.v("geofences", "Invalid geofence type: $transition")
            }
        }
    }
}

class GeofenceReceiver() : KoinComponent {
    private val placesRepo: PlacesRepository by inject()
    private val scopeProvider: ICoroutineScopeProvider by inject()

    fun notifyPlaceEnter(placeIds: List<String>) {
        scopeProvider.userSessionIOScope.launch {
            placesRepo.placesEnter(placeIds)
        }
    }

    fun notifyPlaceExit(placeIds: List<String>) {
        scopeProvider.userSessionIOScope.launch {
            placesRepo.placesExit(placeIds)
        }
    }

}