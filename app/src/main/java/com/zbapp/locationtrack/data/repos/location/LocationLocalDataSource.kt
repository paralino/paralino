package com.zbapp.locationtrack.data.repos.location

import com.zbapp.location.models.location.BaseLocationModel
import com.zbapp.location.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.models.location.LocationEntity
import com.zbapp.locationtrack.data.models.location.LocationMapper
import kotlinx.coroutines.flow.*

class LocationLocalDataSource(
    private val dao: LocationDao
): ILocationLocalDataSource {

    override fun getLocationsForCircle(circleId: String): Flow<List<BaseLocationModel>> {
        return dao.getLocationsForCircle(circleId).mapLocation()
    }

    override fun getLocationsForUser(userId: String): Flow<List<BaseLocationModel>> {
        return dao.getLocationsForUser(userId).mapLocation()
    }

    override suspend fun addLocation(locationModel: BaseLocationModel) {
        dao.saveOnlyOneLocation(LocationMapper.mapReversed(locationModel))
    }

    override suspend fun addLocations(locations: List<BaseLocationModel>) {
        dao.saveOnlyOneLocation(*locations.map { LocationMapper.mapReversed(it) }.toTypedArray())
    }

    override suspend fun clearAll() {
        dao.clear()
    }

    private fun Flow<List<LocationEntity>>.mapLocation(): Flow<List<BaseLocationModel>> {
        return map { it.map { LocationMapper.map(it) }.toTypedArray() }
            .distinctUntilChanged { old, new ->
                old.contentEquals(new)
            }.map { it.toList() }
    }

}