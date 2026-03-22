package app.paralino.core.domain.location

import app.paralino.core.domain.location.engine.LocationSaveEventProcessingEngine
import app.paralino.core.domain.location.engine.LocationUploadEventProcessingEngine
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

class LocationUploadEngine constructor(
    private val locationSharingManager: LocationSharingManager,
    private val registerDeviceForSharing: IReRegisterDeviceUseCase,
    private val locationCacheRepo: LocationCacheRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val oneTimeLocationProvider: IOneTimeLocationProvider,
    private val locationSaveProcessing: LocationSaveEventProcessingEngine,
    private val locationUploadProcessing: LocationUploadEventProcessingEngine,
) {

    private var locationStatusMonitoringJob: Job? = null
    private val _pendingLocation = MutableStateFlow<LocationDataModel?>(null)

    fun startLocationUploadEngine() {
        locationStatusMonitoringJob?.cancel()
        locationStatusMonitoringJob = locationSharingManager.sharingStatus
            .mapNotNull { it.data }
            .onEach { sharingActive ->
                if (sharingActive) {
                    // resetting pending location from any cached value
                    _pendingLocation.value = null
                    locationSaveProcessing.start(
                        pendingLocationFlow = _pendingLocation
                    )

                    locationUploadProcessing.start(
                        pendingLocationFlow = _pendingLocation
                    )
                } else {
                    locationSaveProcessing.stop()
                    locationUploadProcessing.stop()
                }
            }
            .launchIn(scopeProvider.defaultIOScope)
    }

    fun registerNewLocation(data: LocationDataModel) {
        val sharingActive = locationSharingManager.isSharingActiveMaybe()
        logg.d("location_service", "Register new location (active: $sharingActive)")

        if (!sharingActive) {
            throw NoValidContractsException()
        }

        _pendingLocation.update { current ->
            LocationDataModel.getBetter(current, data)
        }
    }

    suspend fun updateDeviceStatusOnly(tryToFetchCachedLocation: Boolean = true) {
        locationUploadProcessing.checkAndUploadLocation(
            data = if (tryToFetchCachedLocation) {
                oneTimeLocationProvider.getLastLocation(
                    requestType = IOneTimeLocationProvider.RequestType.Fetch(
                        timeoutMillis = 3.seconds.inWholeMilliseconds,
                        type = LocationPerformanceType.BATTERY_SAVER
                    )
                )
            } else {
                null
            },
            saveLocally = true
        )
    }

    fun logout() {
        locationUploadProcessing.stop()
        locationSaveProcessing.stop()
        _pendingLocation.update { null }
        locationCacheRepo.clearCache()
        registerDeviceForSharing.logout()
    }
}
