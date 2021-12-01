package com.zbapp.core.repo.location

import com.zbapp.core.config
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.mappers.IMemberMapperCore
import com.zbapp.core.models.mappers.decryptList
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.repo.group.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val groupsRepos: GroupRepository,
    private val mapper: IMemberMapperCore
) {

    fun getLocationsForGroup(groupId: String): Flow<List<MemberModel>> {
        return local.getMembersForGroup(groupId).decryptList(mapper)
    }

    fun pushLocation(data: LocationDataModel): Flow<Unit> {
        return flow {
            val ids = sharingManager.allValidContracts().map { it.objectId }

            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val document = remote.saveCurrentLocation(
                mapper.core2Remote(
                    MemberModel.fromSystem(config.userId, "name", null, data)
                ),
                ids
            )
            local.createOrUpdateMembers(document)
        }
    }

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }


}

