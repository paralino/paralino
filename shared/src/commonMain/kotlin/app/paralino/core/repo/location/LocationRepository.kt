package app.paralino.core.repo.location

import app.paralino.core.domain.crypto.MemberCryptoKeyRotation
import app.paralino.core.domain.crypto.MemberDeviceKeyRotation
import app.paralino.core.domain.location.MemberDeviceStatusProvider
import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.location.MemberModel
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.extensions.safeLet
import kotlinx.coroutines.flow.Flow

class LocationRepository constructor(
    private val sharingManager: LocationSharingManager,
    private val local: LocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val placesRepo: PlacesRepository,
    private val memberIdUseCase: UserMemberIdUseCase,
    private val memberDeviceStatusProvider: MemberDeviceStatusProvider,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase,
    private val memberCryptoKeyRotation: MemberCryptoKeyRotation,
    private val deviceKeyRotation: MemberDeviceKeyRotation
) : BaseRepository() {

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
        contract: LocationSharingContract?
    ): MemberGroupId? {
        return triggerRemoteOnly {
            // skip location update if key rotation is in progress
            if (
                memberCryptoKeyRotation.isProcessing(groupId) ||
                deviceKeyRotation.isProcessing(groupId)
            ) {
                return@triggerRemoteOnly null
            }

            var locationId =
                memberIdUseCase.getCurrentLocationIdForGroup(groupId = groupId)

            if (locationId.isNullOrBlank()) {
                logg.d("Location is null for ${groupId}, try register new device")
                try {
                    registerDeviceForSharing.registerDevice(
                        groupId = groupId,
                        deviceName = null
                    )
                } catch (e: Exception) {
                    // TODO notify user
                    logg.w("Error registering device for group sharing", e)
                    return@triggerRemoteOnly null
                }
            }

            locationId =
                memberIdUseCase.getCurrentLocationIdForGroup(groupId = groupId)

            val member =
                memberIdUseCase.getCurrentUserMemberForGroup(groupId = groupId)

            if (locationId.isNullOrBlank() || member == null) {
                logg.d("Location device can not be established for current device, abort sharing")
                sharingManager.stopSharing(groupId)
                null
            } else if (!member.isVerified) {
                // TODO Notify user and keep sharing contract active for cases where data is not synced yet
                // that way sharing could continue after refresh if member becomes verified
                logg.e("Current member is unverified in group ${groupId}, ignore location update")
                null
            } else {
                MemberGroupId(
                    locationId = locationId,
                    groupId = groupId,
                    member = member,
                    // TODO share approximate location if selected
                    locationData = safeLet(data, contract) { data, contract ->
                        LocationDataModel(
                            timeUTC = data.timeUTC,
                            endTimeUTC = contract.endTimeUTC,
                            lat = data.lat,
                            lng = data.lng,
                            accuracy = data.accuracy,
                            batteryLevel = data.batteryLevel?.takeIf { contract.shareBattery },
                            speed = data.speed?.takeIf { contract.shareSpeed && it != 0f },
                            altitude = data.altitude?.takeIf { contract.shareElevation && it != 0f }
                        )
                    },
                    deviceStatus = memberDeviceStatusProvider.getStatus(groupId = groupId)
                )
            }
        }
    }

    suspend fun pushDeviceStatus(groupId: String) {
        triggerRemoteOnly {
            val payload = getLocationPayload(
                groupId = groupId,
                data = null,
                contract = null
            )

            if (payload != null) {
                remote.saveCurrentLocation(payload)
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

            data?.let { placesRepo.processLocation(it) }
            remote.saveCurrentLocation(mapped)
        }
    }

    fun clearLocation(locationId: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.clearLocation(locationId) },
            localUpdate = { local.clearMemberLocationData(locationId = locationId) }
        )
    }
}
