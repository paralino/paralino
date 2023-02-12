package app.paralino.core.repo.location

import app.paralino.core.domain.location.RegisterDeviceForSharingUseCase
import app.paralino.core.domain.location.UserMemberIdUseCase
import app.paralino.core.logg
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.flow.Flow

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val placesRepo: PlacesRepository,
    private val memberIdUseCase: UserMemberIdUseCase,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase
) : BaseRepository() {

    fun getMembersByUserId(userid: String): Flow<List<MemberModelWithLocations>> {
        return local.getMemberModelsForUser(userid)
    }

    suspend fun getMember(groupId: String, userId: String): MemberModelWithLocations? {
        return local.getMemberOnce(groupId, userId)
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

                if (locationId.isNullOrBlank()) {
                    logg.d("Location device can not be established for current device, abort sharing")
                    sharingManager.stopSharing(contract.groupId)
                    null
                } else {
                    MemberGroupId(
                        locationId = locationId,
                        groupId = contract.groupId,
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

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }

}

