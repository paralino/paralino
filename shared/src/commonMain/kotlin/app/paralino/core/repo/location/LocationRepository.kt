package app.paralino.core.repo.location

import app.paralino.core.domain.location.LocationPayloadProvider
import app.paralino.core.domain.location.LocationPayloadStatus
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.location.MemberModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class LocationRepository constructor(
    private val sharingManager: LocationSharingManager,
    private val performanceManager: LocationPerformanceManager,
    private val local: LocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val placesRepo: PlacesRepository,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase,
    private val payloadProvider: LocationPayloadProvider
) : BaseRepository() {

    private val deviceRegisterTimeMillis = MutableStateFlow(mapOf<String, Long>())
    private var failedRegistrationAttempts = MutableStateFlow(mapOf<String, Int>())

    private val _lastLocation = MutableStateFlow<LocationDataModel?>(null)
    val lastLocation = _lastLocation.asStateFlow()

    suspend fun getMembersByUserId(userid: String): List<MemberModel> {
        return local.getMemberModelsForUser(userid)
    }

    suspend fun getMember(groupId: String, userId: String): MemberModel? {
        return local.getMemberOnce(groupId, userId)
    }

    fun getLocationCountForMember(memberId: String): Flow<Int> {
        return local.getLocationCountForMember(memberId)
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
                try {
                    val timeBetweenRequests = nowUTC() - Instant.fromEpochMilliseconds(
                        epochMilliseconds = deviceRegisterTimeMillis.value[groupId] ?: 0
                    )
                    if (timeBetweenRequests > 10.minutes) {
                        logg.v("Missing user location document for new location, try registering device")
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
                        logg.v("Device registered, retry payload")
                        getLocationPayload(
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
                            // TODO notify user about failed attempts
                        }
                        null
                    }
                } catch (e: Exception) {
                    // TODO notify user
                    logg.w("Error registering device for group sharing", e)
                    null
                }
            }

            LocationPayloadStatus.MissingData -> {
                logg.d("Location device can not be established for current device, abort sharing")
                sharingManager.stopSharing(groupId)
                null
            }

            LocationPayloadStatus.Processing -> null
            LocationPayloadStatus.UnverifiedMember -> {
                // TODO Notify user and keep sharing contract active for cases where data is not synced yet
                // that way sharing could continue after refresh if member becomes verified
                logg.e("Current member is unverified in group ${groupId}, ignore location update")
                null
            }
        }
    }

    suspend fun pushLocation(data: LocationDataModel?) {
        triggerRemoteOnly {
            if (!sharingManager.isSharingActive())
                throw NoValidContractsException()

            val contracts = sharingManager.allValidContracts()
            val mapped = contracts.mapNotNull { contract ->
                getLocationPayload(
                    groupId = contract.groupId,
                    data = data,
                    contract = contract
                )
            }

            if (data != null) {
                _lastLocation.update { data }
                logg.v("Process new location in places")
                placesRepo.processLocation(data)
            }

            logg.v("Upload new location with performance ${performanceManager.locationPerformanceType.value.current}")
            remote.saveCurrentLocation(mapped)
        }
    }

    fun clearLocation(locationId: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.clearLocation(locationId) },
            localUpdate = { local.clearMemberLocationData(locationId = locationId) }
        )
    }

    fun logout() {
        failedRegistrationAttempts.update { emptyMap() }
        deviceRegisterTimeMillis.update { emptyMap() }
    }
}
