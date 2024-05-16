package app.paralino.core.domain.location

import app.paralino.core.MR
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.notifications.IInfoMessagesNotificationPresenter
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.provider.IOneTimeLocationProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRemoteSource
import app.paralino.core.repo.places.PlaceActivityDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LocationUploadEngine constructor(
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val remote: LocationRemoteSource,
    private val placesRepo: PlacesRepository,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase,
    private val payloadProvider: LocationPayloadProvider,
    private val infoNotificationPresenter: IInfoMessagesNotificationPresenter,
    private val locationCacheRepo: LocationCacheRepository,
    private val scopeProvider: ICoroutineScopeProvider,
    private val oneTimeLocationProvider: IOneTimeLocationProvider,
    private val placeActivityRepo: PlaceActivityDataSource
) : LimitedExecutionRepository() {

    private val maxUploadRate: Duration
        get() {
            return when (locationPerformanceManager.locationPerformanceType.value.current) {
                LocationPerformanceType.BATTERY_SAVER -> 10.seconds

                LocationPerformanceType.BALANCED,
                LocationPerformanceType.MEDIUM_HIGH -> 4.seconds

                LocationPerformanceType.HIGH,
                LocationPerformanceType.VERY_HIGH,
                LocationPerformanceType.MANUAL_BOOST -> 2.seconds
            }
        }
    private var locationUploadMonitoringJob: Job? = null
    private var lastLocationPush = MutableStateFlow<Instant?>(null)

    private val _pendingLocation = MutableStateFlow<LocationDataModel?>(null)
    val pendingLocation = _pendingLocation.asStateFlow()

    private val deviceRegisterTimeMillis = MutableStateFlow(mapOf<String, Long>())
    private var failedRegistrationAttempts = MutableStateFlow(mapOf<String, Int>())

    fun startLocationUploadEngine() {
        locationSharingManager.sharingStatus
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
                pendingLocation.filterNotNull()
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

                        lastLocationPush.update { nowUTC() }
                        locationCacheRepo.updateCache(it)
                        scopeProvider.userSessionIOScope.launch {
                            try {
                                uploadLocation(it)
                            } catch (e: Exception) {
                                lastLocationPush.update { null }
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
        val sharingActive = locationSharingManager.isSharingActive()
        logg.d("location_service", "Register new location (active: $sharingActive)")

        if (!sharingActive) {
            throw NoValidContractsException()
        }

        _pendingLocation.update { current ->
            LocationDataModel.getBetter(current, data)
        }
    }

    suspend fun updateDeviceStatusOnly() {
        uploadLocation(
            data = oneTimeLocationProvider.getLastLocation(
                requestType = IOneTimeLocationProvider.RequestType.Fetch(
                    timeoutMillis = 3.seconds.inWholeMilliseconds,
                    type = LocationPerformanceType.BATTERY_SAVER
                )
            )
        )
    }

    private suspend fun uploadLocation(data: LocationDataModel?) {
        triggerRemoteOnly {
            val contracts = locationSharingManager.allValidContracts()
            val mapped = contracts.mapNotNull { contract ->
                getLocationPayload(
                    groupId = contract.groupId,
                    data = data,
                    contract = contract
                )
            }

            if (data != null) {
                placesRepo.processLocation(data)
            }

            logg.v(
                tag = "location_service",
                message = "Upload new ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) with performance ${locationPerformanceManager.locationPerformanceType.value.current}"
            )
            remote.saveCurrentLocation(mapped)
            logg.v(
                tag = "location_service",
                message = "New ${if (data != null) "location ${data.timeUTC}" else "status"} (${data.hashCode()}) uploaded"
            )
        }
    }

    private suspend fun getLocationPayload(
        groupId: String,
        data: LocationDataModel?,
        contract: LocationSharingContract?,
    ): MemberGroupId? {

        val payload = payloadProvider.getLocationPayload(
            groupId = groupId,
            data = data,
            contract = contract
        )
        return when (payload) {
            is LocationPayloadStatus.Success -> payload.payload

            LocationPayloadStatus.MissingLocationDocument -> {
                tryRegisterDeviceAgain(
                    groupId = groupId,
                    data = data,
                    contract = contract
                )
            }

            LocationPayloadStatus.MissingData -> {
                logg.d(
                    "location_service",
                    "Location device can not be established for current device, abort sharing"
                )
                locationSharingManager.stopSharing(groupId)
                placeActivityRepo.clearForGroup(groupId = groupId)
                null
            }

            LocationPayloadStatus.Processing -> null
            LocationPayloadStatus.UnverifiedMember -> {
                // Notify user and keep sharing contract active for cases where data is not synced yet
                // that way sharing could continue after refresh if member becomes verified
                logg.e(
                    "location_service",
                    "Current member is unverified in group ${groupId}, ignore location update"
                )
                infoNotificationPresenter.notify(
                    notificationId = 6345,
                    title = MR.strings.error_something_wrong.localizedString(),
                    message = MR.strings.error_unverified_member_notification_message.localizedString(),
                    timeMillis = null,
                    timeInfo = null
                )
                null
            }
        }
    }

    private suspend fun tryRegisterDeviceAgain(
        groupId: String,
        data: LocationDataModel?,
        contract: LocationSharingContract?,
    ): MemberGroupId? {
        try {
            val timeBetweenRequests = nowUTC() - Instant.fromEpochMilliseconds(
                epochMilliseconds = deviceRegisterTimeMillis.value[groupId] ?: 0
            )
            if (timeBetweenRequests > 10.minutes) {
                logg.v(
                    "location_service",
                    "Missing user location document for new location, try registering device"
                )
                failedRegistrationAttempts.update {
                    it.toMutableMap().apply { set(groupId, 0) }
                }
                deviceRegisterTimeMillis.update {
                    it.toMutableMap().apply { set(groupId, nowUTC().toEpochMilliseconds()) }
                }
                registerDeviceForSharing.registerDevice(
                    groupId = groupId,
                    deviceName = null,
                    contract = contract,
                    notifyOthers = false
                )
                logg.v("location_service", "Device registered, retry payload")
                return getLocationPayload(
                    groupId = groupId,
                    data = data,
                    contract = contract
                )
            } else {
                failedRegistrationAttempts.update {
                    it.toMutableMap().apply { set(groupId, this[groupId]?.plus(1) ?: 1) }
                }
                if ((failedRegistrationAttempts.value[groupId] ?: 0) > 3) {
                    failedRegistrationAttempts.update {
                        it.toMutableMap().apply { set(groupId, 0) }
                    }
                    infoNotificationPresenter.notify(
                        notificationId = 54343,
                        title = MR.strings.error_something_wrong.localizedString(),
                        message = MR.strings.error_device_registration_notification_message.localizedString(),
                        timeMillis = null,
                        timeInfo = null
                    )
                }
                return null
            }
        } catch (e: Exception) {
            logg.w("location_service", "Error registering device for group sharing", e)
            infoNotificationPresenter.notify(
                notificationId = 54343,
                title = MR.strings.error_something_wrong.localizedString(),
                message = MR.strings.error_device_registration_notification_message.localizedString(),
                timeMillis = null,
                timeInfo = null
            )
            return null
        }
    }

    fun logout() {
        locationUploadMonitoringJob?.cancel()
        locationUploadMonitoringJob = null
        _pendingLocation.update { null }
        locationCacheRepo.clearCache()
        failedRegistrationAttempts.update { emptyMap() }
        deviceRegisterTimeMillis.update { emptyMap() }
    }
}
