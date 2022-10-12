package app.paralino.android.tracking.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.launch
import logv
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val receiver: GeofenceReceiver by lazy { GeofenceReceiver() }

    override fun onReceive(context: Context?, intent: Intent?) {

        intent ?: return
        context ?: return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            logv("location_service", "Geofence error: $errorMessage")
            return
        }

        when (val transition = geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                logv("location_service", "User entered geofence")
                receiver.notifyPlaceEnter(geofencingEvent.triggeringGeofences.map { it.requestId })
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                logv("location_service", "User exited geofence")
                receiver.notifyPlaceExit(geofencingEvent.triggeringGeofences.map { it.requestId })
            }
            else -> {
                logv("location_service", "Invalid geofence type: $transition")
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