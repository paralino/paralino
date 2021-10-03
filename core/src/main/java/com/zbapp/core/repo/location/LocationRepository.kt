package com.zbapp.core.repo.location

import com.zbapp.core.config
import com.zbapp.core.models.IDataMapperCore
import com.zbapp.core.models.decryptList
import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.models.location.PostLocationModel
import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.repo.group.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val groupsRepos: GroupRepository,
    private val mapper: IDataMapperCore<BaseLocationModel, SimpleDocumentRaw>
) {

    fun getLocationsForGroup(groupId: String): Flow<List<BaseLocationModel>> {
        return groupsRepos.getGroup(groupId).flatMapLatest {
            local.getLocationsForUsers(it.userIds)
        }.decryptList(mapper)
    }

    fun pushLocation(data: PostLocationModel): Flow<Unit> {
        return flow {
            val ids = sharingManager.allValidContracts().map { it.objectId }

            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val document = remote.saveCurrentLocation(
                mapper.core2Remote(
                    BaseLocationModel.fromSystem("", config.userId, "name", null, data)
                ),
                ids
            )
            local.addLocation(document)
        }
    }

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }


}

