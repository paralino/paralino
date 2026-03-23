package app.paralino.core.domain.location.engine

import app.paralino.core.domain.location.BatteryAlertUploadEngine
import app.paralino.core.domain.location.SaveLocationUseCase
import app.paralino.core.domain.location.history.SavePendingLocationUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppHost
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.IPlacesActivityProcessor
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LocationUploadEventProcessingEngine(
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfigRepository,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager,
    private val locationPayloadRequest: HandleGetLocationPayloadRequest,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val savePendingLocationUseCase: SavePendingLocationUseCase,
    private val placesActivityProcessor: IPlacesActivityProcessor,
    private val batteryAlertUploadEngine: BatteryAlertUploadEngine,
) : ILocationEventProcessingEngine(scope = scopeProvider.userSessionIOScope) {
    override val tag: String = "upload"
    override val processingRate: Duration
        get() {
            return when (appConfig.appHost) {
                AppHost.MAIN_APP,
                AppHost.IOS_NOTIFICATION_EXTENSION -> {
                    when (locationPerformanceManager.locationPerformanceType.value.current) {
                        LocationPerformanceType.HIGH_BATTERY_SAVER -> 60.seconds
                        LocationPerformanceType.BATTERY_SAVER -> 60.seconds
                        LocationPerformanceType.BALANCED -> 40.seconds
                        LocationPerformanceType.MEDIUM_HIGH -> 10.seconds
                        LocationPerformanceType.HIGH,
                        LocationPerformanceType.VERY_HIGH,
                        LocationPerformanceType.MANUAL_BOOST -> 2.seconds
                    }
                }

                AppHost.IOS_LOCATION_EXTENSION -> 2.seconds
            }
        }

    override fun eventAction(locationData: LocationDataModel) {
        scopeProvider.userSessionIOScope.launch {
            try {
                checkAndUploadLocation(data = locationData, saveLocally = false)
            } catch (e: Exception) {
//              lastLocationPush.update { null }
                logg.w(
                    tag = "location_service",
                    message = "Error uploading new location: ${e.message}"
                )
            }
        }
    }

    suspend fun checkAndUploadLocation(
        data: LocationDataModel?,
        saveLocally: Boolean
    ) {
        val contracts = locationSharingManager.allValidContracts()
        val payloads = contracts.mapNotNull { contract ->
            locationPayloadRequest.getLocationPayload(
                groupId = contract.groupOrSpaceId,
                data = data,
                contract = contract
            )
        }

        if (saveLocally) {
            // save location to history locally
            try {
                savePendingLocationUseCase.saveNewLocations(payloads)
            } catch (e: Exception) {
            }
        }

        // location upload
        val uploadError = try {
            triggerRemoteOnly {
                logg.v(
                    tag = "location_service",
                    message = "Upload (${payloads.size}) new ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) with performance ${locationPerformanceManager.locationPerformanceType.value.current}"
                )

                payloads.forEach {
                    try {
                        saveLocationUseCase.uploadAndSaveCurrentLocation(
                            MemberGroupIdForUpload(it)
                        )
                    } catch (e: Exception) {
                        logg.w(
                            tag = "location_service",
                            message = "Error saving location for group ${it.groupId}",
                            t = e
                        )
                    }
                }

                logg.v(
                    tag = "location_service",
                    message = "New ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) uploaded"
                )
            }
            null
        } catch (e: Exception) {
            e
        }

        // extra processing
        triggerRemoteOnly {
            if (data != null) {
                placesActivityProcessor.processLocation(data)
            }

            batteryAlertUploadEngine.sendBatteryAlert(payloads = payloads)
        }

        if (uploadError != null) {
            throw uploadError
        }
    }
}