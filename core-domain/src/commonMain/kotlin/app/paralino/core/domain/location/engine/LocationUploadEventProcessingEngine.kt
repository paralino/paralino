package app.paralino.core.domain.location.engine

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.SaveLocationUseCase
import app.paralino.core.domain.location.history.SavePendingLocationUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppHost
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.provider.AppLifecycleType
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.base.AppConfigRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.repo.location.LocationSharingContractFilter
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LocationUploadEventProcessingEngine(
    private val scopeProvider: ICoroutineScopeProvider,
    private val appConfig: AppConfigRepository,
    private val appPrefs: AppPrefs,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val locationSharingManager: LocationSharingManager,
    private val locationPayloadRequest: HandleGetLocationPayloadRequest,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val savePendingLocationUseCase: SavePendingLocationUseCase,
    private val sharingContractFilter: LocationSharingContractFilter,
    private val deviceApiProvider: IDeviceApiProvider
) : ILocationEventProcessingEngine(scope = scopeProvider.userSessionIOScope) {

    override val tag: String = "upload"
    override val processingRate: Duration
        get() {
            return when (appConfig.appHost) {
                AppHost.MAIN_APP,
                AppHost.IOS_NOTIFICATION_EXTENSION -> {
                    when (locationPerformanceManager.locationPerformanceType.value.current) {
                        LocationPerformanceType.HIGH_BATTERY_SAVER -> 120.seconds
                        LocationPerformanceType.BATTERY_SAVER -> 90.seconds
                        LocationPerformanceType.BALANCED -> {
                            when (deviceApiProvider.appLifecycleState.value.current) {
                                AppLifecycleType.ACTIVE -> 40.seconds
                                AppLifecycleType.INACTIVE,
                                AppLifecycleType.BACKGROUND,
                                AppLifecycleType.DESTROYED -> 90.seconds
                            }
                        }

                        LocationPerformanceType.MEDIUM_HIGH -> 15.seconds
                        LocationPerformanceType.HIGH,
                        LocationPerformanceType.HIGHER_FOCUS,
                        LocationPerformanceType.VERY_HIGH,
                        LocationPerformanceType.MANUAL_BOOST -> 2.seconds
                    }
                }

                AppHost.IOS_LOCATION_EXTENSION -> 2.seconds
            }
        }

    private val secondaryGroupLastUploadEvent = MutableStateFlow<Instant?>(null)
    private val secondaryGroupBackoffRate: Duration
        get() {
            return when (locationPerformanceManager.locationPerformanceType.value.current) {
                LocationPerformanceType.HIGH_BATTERY_SAVER -> 5.minutes
                LocationPerformanceType.BATTERY_SAVER -> 3.minutes
                LocationPerformanceType.BALANCED -> {
                    val multiplier = when (locationPerformanceManager.locationPerformanceType.value.balancedPerformanceSettings.config) {
                        LocationPerformanceSettingsType.Balanced.Level1,
                        LocationPerformanceSettingsType.Balanced.Level2 -> 3.0
                        LocationPerformanceSettingsType.Balanced.Level3 -> 2.0
                        LocationPerformanceSettingsType.Balanced.Level4,
                        LocationPerformanceSettingsType.Balanced.Level5 -> 1.0
                    }
                    when (deviceApiProvider.appLifecycleState.value.current) {
                        AppLifecycleType.ACTIVE,
                        AppLifecycleType.INACTIVE -> 1.minutes.times(multiplier)
                        AppLifecycleType.BACKGROUND,
                        AppLifecycleType.DESTROYED -> 2.minutes.times(multiplier)
                    }
                }

                LocationPerformanceType.MEDIUM_HIGH -> 1.minutes
                LocationPerformanceType.HIGH -> {
                    val multiplier = when (locationPerformanceManager.locationPerformanceType.value.balancedPerformanceSettings.config) {
                        LocationPerformanceSettingsType.Balanced.Level1,
                        LocationPerformanceSettingsType.Balanced.Level2 -> 2.0
                        LocationPerformanceSettingsType.Balanced.Level3 -> 1.5
                        LocationPerformanceSettingsType.Balanced.Level4,
                        LocationPerformanceSettingsType.Balanced.Level5 -> 1.0
                    }
                    when (deviceApiProvider.appLifecycleState.value.current) {
                        AppLifecycleType.ACTIVE,
                        AppLifecycleType.INACTIVE -> 20.seconds.times(multiplier)
                        AppLifecycleType.BACKGROUND,
                        AppLifecycleType.DESTROYED -> 40.seconds.times(multiplier)
                    }
                }

                LocationPerformanceType.HIGHER_FOCUS,
                LocationPerformanceType.VERY_HIGH -> {
                    val multiplier = when (locationPerformanceManager.locationPerformanceType.value.balancedPerformanceSettings.config) {
                        LocationPerformanceSettingsType.Balanced.Level1,
                        LocationPerformanceSettingsType.Balanced.Level2 -> 2.0
                        LocationPerformanceSettingsType.Balanced.Level3 -> 1.5
                        LocationPerformanceSettingsType.Balanced.Level4,
                        LocationPerformanceSettingsType.Balanced.Level5 -> 1.0
                    }
                    30.seconds.times(multiplier)
                }

                LocationPerformanceType.MANUAL_BOOST -> 0.seconds
            }
        }

    override fun eventAction(locationData: LocationDataModel) {
        scopeProvider.userSessionIOScope.launch {
            try {
                checkAndUploadLocation(
                    data = locationData,
                    bypassFilter = false,
                    saveLocally = appConfig.appHost == AppHost.IOS_LOCATION_EXTENSION
                )
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
        bypassFilter: Boolean,
        saveLocally: Boolean
    ) {
        val allValidContracts = locationSharingManager.allValidContracts()
        val filteredContracts = allValidContracts
            .filter { contract -> bypassFilter || canUseContractForThisUpdate(contract = contract) }

        val filteredPayloads = filteredContracts.mapNotNull { contract ->
            locationPayloadRequest.getLocationPayload(
                groupId = contract.groupOrSpaceId,
                data = data,
                contract = contract
            )
        }

        if (saveLocally) {
            // save location to history locally
            try {
                savePendingLocationUseCase.saveNewLocations(filteredPayloads)
            } catch (e: Exception) {
            }
        }

        // location upload
        val uploadError = try {
            triggerRemoteOnly {
                logg.v(
                    tag = "location_service",
                    message = "Upload (${filteredPayloads.size}) new ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) with performance ${locationPerformanceManager.locationPerformanceType.value.current}"
                )

                saveLocationUseCase.uploadAndSaveCurrentLocations(
                    items = filteredPayloads.map { MemberGroupIdForUpload(it) }
                )

                logg.v(
                    tag = "location_service",
                    message = "New ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) uploaded"
                )
            }
            null
        } catch (e: Exception) {
            e
        }

        if (uploadError != null) {
            throw uploadError
        } else {
            if (allValidContracts.size == filteredContracts.size || secondaryGroupLastUploadEvent.value == null) {
                // only marking as upload event if there are secondary groups
                secondaryGroupLastUploadEvent.update { nowUTC() }
            }
        }
    }

    private fun canUseContractForThisUpdate(contract: LocationSharingContract): Boolean {
        val filterResult = sharingContractFilter.contractFilterResult(contract = contract)
        return when (appConfig.appHost) {
            AppHost.MAIN_APP -> {
                // in main app we allow secondary groups to send events periodically
                val isAllowedInTimeWindow =
                    secondaryGroupLastUploadEvent.value?.isOlderThan(secondaryGroupBackoffRate) != false

                when (deviceApiProvider.appLifecycleState.value.current) {
                    AppLifecycleType.ACTIVE,
                    AppLifecycleType.INACTIVE -> {
                        val isSelected = if (appPrefs.isPersonalSpaceSelected) {
                            contract.groupOrSpaceId.isPersonalSpace
                        } else {
                            appPrefs.selectedGroupId == contract.groupOrSpaceId.id
                        }

                        isSelected || filterResult.isExplicitlyAllowed || isAllowedInTimeWindow
                    }

                    AppLifecycleType.BACKGROUND,
                    AppLifecycleType.DESTROYED -> filterResult.isEmptyOrAllowed || isAllowedInTimeWindow
                }
            }

            AppHost.IOS_NOTIFICATION_EXTENSION,
            AppHost.IOS_LOCATION_EXTENSION -> filterResult.isEmptyOrAllowed
        }
    }

    override fun stop() {
        super.stop()
        secondaryGroupLastUploadEvent.update { null }
    }
}