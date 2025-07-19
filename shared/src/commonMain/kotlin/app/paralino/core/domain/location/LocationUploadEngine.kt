package app.paralino.core.domain.location

import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.location.history.SavePendingLocationUseCase
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.IPlacesActivityProcessor
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.utils.exceptions.DeviceSafetySignatureCorrupted
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LocationUploadEngine constructor(
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val savePendingLocationUseCase: SavePendingLocationUseCase,
    private val placesActivityProcessor: IPlacesActivityProcessor,
    private val registerDeviceForSharing: IReRegisterDeviceUseCase,
    private val payloadProvider: LocationPayloadProvider,
    private val locationCacheRepo: LocationCacheRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val oneTimeLocationProvider: IOneTimeLocationProvider,
    private val placeActivityRepo: PlaceActivityDataSource,
    private val batteryAlertUploadEngine: BatteryAlertUploadEngine,
    private val appBuildConfig: AppBuildConfig,
) : LimitedExecutionRepository() {

    private val maxUploadRate: Duration
        get() {
            when (appBuildConfig.appHost) {
                AppHost.MAIN_APP,
                AppHost.IOS_NOTIFICATION_EXTENSION -> {
                    return when (locationPerformanceManager.locationPerformanceType.value.current) {
                        LocationPerformanceType.HIGH_BATTERY_SAVER -> 20.seconds
                        LocationPerformanceType.BATTERY_SAVER -> 10.seconds

                        LocationPerformanceType.BALANCED,
                        LocationPerformanceType.MEDIUM_HIGH -> 4.seconds

                        LocationPerformanceType.HIGH,
                        LocationPerformanceType.VERY_HIGH,
                        LocationPerformanceType.MANUAL_BOOST -> 2.seconds
                    }
                }
                AppHost.IOS_LOCATION_EXTENSION -> return 2.seconds
            }
        }
    private var locationUploadMonitoringJob: Job? = null
    private var locationStatusMonitoringJob: Job? = null
    private var lastLocationPush = MutableStateFlow<Instant?>(null)

    private val _pendingLocation = MutableStateFlow<LocationDataModel?>(null)


    fun startLocationUploadEngine() {
        locationStatusMonitoringJob?.cancel()
        locationStatusMonitoringJob = locationSharingManager.sharingStatus
            .mapNotNull { it.data }
            .onEach { sharingActive ->
                if (sharingActive) {
                    registerLocationUploadJob()
                } else {
                    locationUploadMonitoringJob?.cancel()
                    locationUploadMonitoringJob = null
                }
            }
            .launchIn(scopeProvider.defaultIOScope)
    }

    private fun registerLocationUploadJob() {
        locationUploadMonitoringJob?.cancel()
        locationUploadMonitoringJob = scopeProvider.userSessionIOScope.launch {
            logg.d(tag = "location_service", message = "Start location upload job")
            try {
                _pendingLocation.value = null
                _pendingLocation.filterNotNull()
                    .conflate()
                    .collectLatest {
                        logg.d(tag = "location_service", message = "Processing new location")
                        val lastUpdate = lastLocationPush.value
                        val diff = if (lastUpdate != null) nowUTC() - lastUpdate else 0.seconds
                        if (diff > Duration.ZERO && diff < maxUploadRate) {
                            val delay = maxUploadRate - diff
                            // no need to lock thread if potential delay is small
                            if (delay > 100.milliseconds) {
                                logg.d(
                                    tag = "location_service",
                                    message = "Delay location upload by $delay"
                                )
                                delay(delay)
                            }
                        }

                        logg.d(
                            tag = "location_service",
                            message = "Location (${it.timeUTC}) ready for upload"
                        )
                        lastLocationPush.update { nowUTC() }
                        locationCacheRepo.updateCache(it)
                        scopeProvider.userSessionIOScope.launch {
                            try {
                                uploadLocation(it)
                            } catch (e: Exception) {
//                                lastLocationPush.update { null }
                                logg.w(
                                    tag = "location_service",
                                    message = "Error uploading new location: ${e.message}"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                logg.w(
                    tag = "location_service",
                    message = "Error processing pending location: ${e.message}",
                    t = e
                )
            }
        }
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
        uploadLocation(
            data = if (tryToFetchCachedLocation) {
                oneTimeLocationProvider.getLastLocation(
                    requestType = IOneTimeLocationProvider.RequestType.Fetch(
                        timeoutMillis = 3.seconds.inWholeMilliseconds,
                        type = LocationPerformanceType.BATTERY_SAVER
                    )
                )
            } else {
                null
            }
        )
    }

    private suspend fun uploadLocation(data: LocationDataModel?) {
        val payloads = triggerRemoteOnly {
            val contracts = locationSharingManager.allValidContracts()
            contracts.mapNotNull { contract ->
                getLocationPayload(
                    groupId = contract.groupId,
                    data = data,
                    contract = contract
                )
            }
        }

        // save location to history locally
        try { savePendingLocationUseCase.saveNewLocations(payloads) }
        catch (e: Exception) { }

        // location upload
        val uploadError = try {
            triggerRemoteOnly {
                logg.v(
                    tag = "location_service",
                    message = "Upload (${payloads.size}) new ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) with performance ${locationPerformanceManager.locationPerformanceType.value.current}"
                )

                payloads.forEach {
                    try {
                        saveLocationUseCase.uploadAndSaveCurrentLocation(it)
                    } catch (e: Exception) {
                        logg.w("location_service", "Error saving location for group ${it}", e)
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

    private suspend fun getLocationPayload(
        groupId: String,
        data: LocationDataModel?,
        contract: LocationSharingContract,
    ): MemberGroupId? {

        val payload = payloadProvider.getLocationPayload(
            groupId = groupId,
            data = data,
            contract = contract
        )
        return when (payload) {
            is LocationPayloadStatus.Success -> payload.payload

            LocationPayloadStatus.MissingLocationDocument -> {
                val result = try {
                    registerDeviceForSharing.tryRegisterDeviceAgain(
                        groupId = groupId,
                        data = data,
                        contract = contract
                    )
                } catch (e: Exception) {
                    if (e is DeviceSafetySignatureCorrupted) {
                        logg.d(
                            "location_service",
                            "Location device safety signature is corrupted, abort sharing"
                        )
                        locationSharingManager.stopSharing(groupId = groupId, deleteContract = false)
                        placeActivityRepo.clearForGroup(groupId = groupId)
                        return null
                    } else {
                        false
                    }
                }

                if (result) {
                    // retry once again
                    getLocationPayload(
                        groupId = groupId,
                        data = data,
                        contract = contract
                    )
                } else {
                    null
                }
            }

            LocationPayloadStatus.MissingData -> {
                logg.d(
                    "location_service",
                    "Location device can not be established for current device, abort sharing"
                )
                locationSharingManager.stopSharing(groupId = groupId, deleteContract = false)
                placeActivityRepo.clearForGroup(groupId = groupId)
                null
            }

            LocationPayloadStatus.Processing -> null
        }
    }

    fun logout() {
        locationUploadMonitoringJob?.cancel()
        locationUploadMonitoringJob = null
        _pendingLocation.update { null }
        locationCacheRepo.clearCache()
        registerDeviceForSharing.logout()
    }
}
