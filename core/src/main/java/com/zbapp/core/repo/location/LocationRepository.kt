package com.zbapp.core.repo.location

import com.zbapp.core.config
import com.zbapp.core.models.Payload
import com.zbapp.core.models.group.UserGroupWithMembers
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberGroupId
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.group.GroupRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val groupsRepos: GroupRepository
) : BaseRepository() {

    fun locationRefresh(groupId: String): Flow<Payload<Unit>> {
        return flow {
            emit(Payload.Loading)
            do {
                try {
                    groupsRepos.refreshGroupWithMembers(groupId).first()
                    emit(Payload.Success(Unit))
                } catch (e: Exception) {
                    emit(Payload.Error(e))
                }
                delay(6_000)
            } while (true)
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
                local.clearMemberLocationData(local.getUserMemberIds(config.userId, groupId).first())
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

