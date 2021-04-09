package com.zbapp.core.repo.location

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.LocationSharingManager
import com.zbapp.core.repo.NoValidContractsException
import com.zbapp.core.utils.randomString
import kotlinx.coroutines.flow.*

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: ILocationRemoteDataSource
) {

    fun getLocationsForCircle(circleId: String): Flow<List<BaseLocationModel>> {
        return local.getLocationsForCircle(circleId)
    }

    fun pushLocation(location: BaseLocationModel): Flow<Unit> {
        return flow {
            val mapped = sharingManager.allValidContracts().map {
                location.copy(id = randomString(), circleId = it.circleId)
            }

            if(mapped.isEmpty()) throw NoValidContractsException()

            local.addLocations(mapped)
            remote.setCurrentLocations(mapped)
        }
    }


}

