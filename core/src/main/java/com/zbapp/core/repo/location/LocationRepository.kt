package com.zbapp.core.repo.location

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.utils.randomString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource
) {

    fun getLocationsForGroup(groupId: String): Flow<List<BaseLocationModel>> {
        return local.getLocationsForGroup(groupId)
    }

    fun pushLocation(location: BaseLocationModel): Flow<Unit> {
        return flow {
            val mapped = sharingManager.allValidContracts().map {
                location.copy(id = randomString(), groupId = it.groupId)
            }

            if(!sharingManager.isSharingActive()) throw NoValidContractsException()

            remote.saveCurrentLocation(location)
            local.addLocations(mapped)
        }
    }

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }


}

