package com.zbapp.core.repo.location

import com.zbapp.core.config
import com.zbapp.core.models.location.LocationDataModel
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.mappers.MemberMapperCore
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.repo.group.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val groupsRepos: GroupRepository,
    private val mapper: MemberMapperCore
) {

    fun getLocationsForGroup(groupId: String): Flow<List<MemberModel>> {
        return local.getMembersForGroup(groupId)
                // TODO add group public key
            .map { it.map { mapper.remote2Core(it, "") } }
    }

    fun pushLocation(data: LocationDataModel): Flow<Unit> {
        return flow {
            val ids = sharingManager.allValidContracts().map { it.objectId }

            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val document = remote.saveCurrentLocation(
                mapper.core2Remote(
                    model = MemberModel.fromSystem(config.userId, "name", null, data),
                    // TODO add group public key
                    groupOwnerPublicKey = ""
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

