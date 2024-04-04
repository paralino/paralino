package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.places.GroupPlaceGeofenceData
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.IGeofenceManager
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.hours

class GeofenceRegistrationMonitor(
    private val geofenceManager: IGeofenceManager,
    private val placesRepo: PlacesRepository,
    private val deviceApiProvider: IDeviceApiProvider,
    private val locationSharingManager: LocationSharingManager,
    private val scopeProvider: ICoroutineScopeProvider
) {
    fun startGeofenceRegistration(reRegisterPeriodically: Boolean) {
        // timer to reregister places after certain time
        val timer = flow {
            emit(Unit)
            if (reRegisterPeriodically) {
                while (currentCoroutineContext().isActive) {
                    delay(20L.hours.inWholeMilliseconds)
                    emit(Unit)
                }
            }
        }

        val places = placesRepo.subscribeToAllActivePlaces()
            .map {
                it.mapNotNull { place ->
                    place.data.data?.let {
                        GroupPlaceGeofenceData(
                            groupId = place.groupId,
                            placeId = place.id,
                            lat = it.lat,
                            lng = it.lng,
                            radiusMeters = it.radiusMeters
                        )
                    }
                }
            }
            .distinctFlowOfList()

        combine(
            places,
            deviceApiProvider.locationStatusProvider.status.map { it.isOK(forAlwaysOnAccess = true) },
            locationSharingManager.sharingStatus.map { it.data }.filterNotNull(),
            timer
        ) { geofences, locationStatus, sharingStatus, _ ->
            logg.v(
                "geofences",
                "App status changed, reinit geofences (sharing: $sharingStatus, app status: $locationStatus)"
            )
            if (locationStatus && sharingStatus) {
                geofenceManager.registerGeofences(geofences)
            } else {
                geofenceManager.deleteAllGeofences()
            }
        }
            .catch {
                if (it is CancellationException) {
                    geofenceManager.deleteAllGeofences()
                }
            }
            .launchIn(scopeProvider.userSessionIOScope)
    }
}