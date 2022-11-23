package app.paralino.core.repo.location

import app.paralino.core.domain.location.UserMemberIdUseCase
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
    private val memberIdUseCase: UserMemberIdUseCase
) : BaseRepository() {

    fun getMembersByUserId(userid: String): Flow<List<MemberModelWithLocations>> {
        return local.getMemberModelsForUser(userid)
    }

    suspend fun pushLocation(data: LocationDataModel) {
        triggerRemoteOnly {
            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val contracts = sharingManager.allValidContracts()
            val mapped = contracts.map { contract ->
                MemberGroupId(
                    locationId = memberIdUseCase.getCurrentDeviceIdForGroup(groupId = contract.groupId),
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

            placesRepo.processLocation(data)
            remote.saveCurrentLocation(mapped)
        }
    }

    // TODO pass specific location
    fun clearLocation(groupId: String): Flow<Unit> {
        return getDataOnce(
            localUpdate = {
                local.clearMemberLocationData(
                    locationId = memberIdUseCase.getCurrentDeviceIdForGroup(groupId)!!
                )
            }
        ) {
            remote.saveCurrentLocation(
                ids = listOf(
                    MemberGroupId(
                        locationId = memberIdUseCase.getCurrentDeviceIdForGroup(groupId)!!,
                        groupId = groupId,
                        data = null
                    )
                )
            )
        }
    }

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }

}

