package app.paralino.core.domain.location.engine

import app.paralino.core.domain.location.LocationCacheRepository
import app.paralino.core.domain.location.history.SavePendingLocationUseCase
import app.paralino.core.models.AppHost
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LocationSaveEventProcessingEngine(
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfigRepository,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager,
    private val locationPayloadRequest: HandleGetLocationPayloadRequest,
    private val savePendingLocationUseCase: SavePendingLocationUseCase,
    private val locationCacheRepo: LocationCacheRepository,
) : ILocationEventProcessingEngine(scope = scopeProvider.userSessionIOScope) {
    override val tag: String = "saving"
    override val processingRate: Duration
        get() {
            return when (appConfig.appHost) {
                AppHost.MAIN_APP,
                AppHost.IOS_NOTIFICATION_EXTENSION -> {
                    when (locationPerformanceManager.locationPerformanceType.value.current) {
                        LocationPerformanceType.HIGH_BATTERY_SAVER -> 20.seconds
                        LocationPerformanceType.BATTERY_SAVER -> 10.seconds
                        LocationPerformanceType.BALANCED -> 6.seconds
                        LocationPerformanceType.MEDIUM_HIGH -> 4.seconds
                        LocationPerformanceType.HIGH,
                        LocationPerformanceType.VERY_HIGH,
                        LocationPerformanceType.MANUAL_BOOST -> 2.seconds
                    }
                }

                AppHost.IOS_LOCATION_EXTENSION -> 2.seconds
            }
        }

    override fun start(pendingLocationFlow: StateFlow<LocationDataModel?>) {
        when (appConfig.appHost) {
            AppHost.MAIN_APP,
            AppHost.IOS_NOTIFICATION_EXTENSION -> {
                super.start(pendingLocationFlow)
            }

            AppHost.IOS_LOCATION_EXTENSION -> {
                return
            }
        }
    }

    override fun eventAction(locationData: LocationDataModel) {
        locationCacheRepo.updateCache(data = locationData)
        scopeProvider.userSessionIOScope.launch {
            checkAndSaveLocationLocally(locationData = locationData)
        }
    }

    suspend fun checkAndSaveLocationLocally(locationData: LocationDataModel?) {
        val contracts = locationSharingManager.allValidContracts()
        val payloads = contracts.mapNotNull { contract ->
            locationPayloadRequest.getLocationPayload(
                groupId = contract.groupOrSpaceId,
                data = locationData,
                contract = contract
            )
        }

        // save location to history locally
        try {
            savePendingLocationUseCase.saveNewLocations(payloads)
        } catch (e: Exception) {
        }
    }
}