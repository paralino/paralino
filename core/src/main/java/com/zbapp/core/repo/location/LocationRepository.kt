package com.zbapp.core.repo.location

import com.zbapp.core.config
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberGroupId
import com.zbapp.core.models.location.MemberModel
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
): BaseRepository() {

    fun locationRefresh(groupId: String): Flow<Unit> {
        return flow<Unit> {
            do {
                groupsRepos.refreshGroupWithMembers(groupId).first()
                delay(6_000)
            }
            while (true)
        }
    }

    fun getLocationsForGroup(groupId: String): Flow<List<MemberModel>> {
        return local.getMembersForGroup(groupId)
    }

    fun pushLocation(data: LocationDataModel): Flow<Unit> {
        return getDataOnce {
            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val ids = sharingManager.allValidContracts().map { it.objectId }
            val mapped = ids.map { groupId ->
                MemberGroupId(
                    memberId = local.getUserMemberIds(config.userId, groupId)
                        // TODO use current device id for multiple device support
                        .first(),
                    groupId = groupId
                )
            }

            remote.saveCurrentLocation(data, mapped)
        }
    }

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }


}

