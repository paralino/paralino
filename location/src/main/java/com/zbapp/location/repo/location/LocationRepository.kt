package com.zbapp.location.repo.location

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.repo.LocationSharingManager
import com.zbapp.location.repo.NoValidContractsException
import com.zbapp.location.utils.randomString
import com.zbapp.location.utils.replace
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
            remote.addLocations(mapped)
        }
    }


}

