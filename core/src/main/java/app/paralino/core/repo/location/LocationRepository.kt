package app.paralino.core.repo.location

import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.Payload
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val groupsRepos: GroupRepository,
    private val realtimeApi: IRealtimeApi,
    private val locationPerformanceManager: LocationPerformanceManager,
) : BaseRepository() {

    fun locationRefresh(groupId: String): Flow<Unit> {

        val locationSubscriptionFlow = realtimeApi.subscribe("databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionGroupMembersID}.documents")
            .onStart {
                groupsRepos.refreshGroupWithMembers(groupId)
            }
            .map {
                try {
                    val member = MemberDocumentRaw.from(it)
                    local.createOrUpdateMembers(member)
                    Payload.Success(member)
                } catch (e: Exception) {
                    logg.e("Error parsing member on map", e)
                    Payload.Error(e)
                }
            }

        val userMapFocusFlow = flow {
            do {
                locationPerformanceManager.boostOnMap()
                emit("User is looking at map")
                delay(4_000)
            } while (currentCoroutineContext().isActive)
        }

        return combine(
            locationSubscriptionFlow,
            userMapFocusFlow
        ) { _, _ ->
            Unit
        }
    }

    fun getLocationsForGroup(groupId: String): Flow<UserGroupWithMembers> {
        return groupsRepos.getGroupWithMembersLocally(groupId)
    }

    fun pushLocation(data: LocationDataModel): Flow<Unit> {
        return getDataOnce {
            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val contracts = sharingManager.allValidContracts()
            val mapped = contracts.map { contract ->
                MemberGroupId(
                    memberId = local.getUserMemberIds(config.userId, contract.groupId)
                        // TODO use current device id for multiple device support
                        .first(),
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

            remote.saveCurrentLocation(mapped)
        }
    }

    fun clearLocation(groupId: String): Flow<Unit> {
        return getDataOnce(
            localUpdate = {
                local.clearMemberLocationData(local.getUserMemberIds(config.userId, groupId)
                    .first())
            }
        ) {
            remote.saveCurrentLocation(
                ids = listOf(
                    MemberGroupId(
                        memberId = local.getUserMemberIds(config.userId, groupId).first(),
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

