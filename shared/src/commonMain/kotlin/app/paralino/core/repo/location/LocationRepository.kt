package app.paralino.core.repo.location

import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.flow.Flow

class LocationRepository constructor(
    private val sharingManager: LocationSharingManager,
    private val local: LocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val placesRepo: PlacesRepository,
    private val memberIdUseCase: UserMemberIdUseCase,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase
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

    suspend fun pushLocation(data: LocationDataModel) {
        triggerRemoteOnly {
            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val contracts = sharingManager.allValidContracts()
            val mapped = contracts.mapNotNull { contract ->

                var locationId =
                    memberIdUseCase.getCurrentLocationIdForGroup(groupId = contract.groupId)

                if (locationId.isNullOrBlank()) {
                    logg.d("Location is null for ${contract.groupId}, try register new device")
                    registerDeviceForSharing.registerDevice(groupId = contract.groupId)
                }

                locationId =
                    memberIdUseCase.getCurrentLocationIdForGroup(groupId = contract.groupId)

                val member = memberIdUseCase.getCurrentUserMemberForGroup(groupId = contract.groupId)

                if (locationId.isNullOrBlank() || member == null) {
                    logg.d("Location device can not be established for current device, abort sharing")
                    sharingManager.stopSharing(contract.groupId)
                    null
                } else if(!member.isVerified) {
                    // TODO Notify user and keep sharing contract active for cases where data is not synced yet
                    // that way sharing could continue after refresh if member becomes verified
                    logg.d("Current member is unverified, stop sharing")
                    null
                } else {
                    MemberGroupId(
                        locationId = locationId,
                        groupId = contract.groupId,
                        member = member,
                        // TODO share approximate location if selected
                        data = LocationDataModel(
                            timeUTC = data.timeUTC,
                            lat = data.lat,
                            lng = data.lng,
                            accuracy = data.accuracy,
                            batteryLevel = data.batteryLevel?.takeIf { contract.shareBattery },
                            speed = data.speed?.takeIf { contract.shareSpeed && it != 0f },
                            altitude = data.altitude?.takeIf { contract.shareElevation && it != 0f }
                        )
                    )
                }
            }

            placesRepo.processLocation(data)
            remote.saveCurrentLocation(mapped)
        }
    }

    fun clearLocation(locationId: String): Flow<Unit> {
        return getDataOnce(
            localUpdate = {
                local.clearMemberLocationData(locationId = locationId)
            }
        ) {
            remote.clearLocation(locationId)
        }
    }

}

