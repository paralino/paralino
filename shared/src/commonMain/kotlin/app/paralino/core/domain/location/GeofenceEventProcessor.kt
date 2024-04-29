package app.paralino.core.domain.location

import app.paralino.core.data.db.AppConfig
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class GeofenceEventProcessor(
    private val placesRepo: PlacesRepository,
    private val locationUploadEngine: LocationUploadEngine,
    private val oneTimeLocationProvider: IOneTimeLocationProvider,
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfig
) {
    fun placesEnter(placesIds: List<String>) {
        sendEvent(placesIds, enter = true)
    }

    fun placesExit(placesIds: List<String>) {
        sendEvent(placesIds, enter = false)
    }

    private fun sendEvent(placesIds: List<String>, enter: Boolean) {
        scopeProvider.userSessionIOScope.launch {
            listOf(
                async {
                    if (!appConfig.placeActivityThrottle) {
                        delay(5.seconds)
                    }
                    fetchAndUploadLocationAndPlace()
                },
                async {
                    if (appConfig.placeActivityThrottle) {
                        delay(4.seconds)
                    }
                    if (enter) {
                        placesRepo.placesEnter(placesIds, fromGeofence = true)
                    } else {
                        placesRepo.placesExit(placesIds, fromGeofence = true)
                    }
                }
            ).awaitAll()
        }
    }

    private suspend fun fetchAndUploadLocationAndPlace() {
        val timeout = if (appConfig.placeActivityThrottle) {
            12.seconds
        } else {
            8.seconds
        }
        val newLocation = oneTimeLocationProvider.getLastLocation(
            requestType = IOneTimeLocationProvider.RequestType.Fetch(
                timeoutMillis = timeout.inWholeMilliseconds,
                type = LocationPerformanceType.HIGH
            )
        )
        if (newLocation != null) {
            locationUploadEngine.registerNewLocation(newLocation)
        }
    }
}