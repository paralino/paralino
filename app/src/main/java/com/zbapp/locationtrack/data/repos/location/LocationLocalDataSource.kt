package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.location.BaseLocationModel
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.mappers.LocationMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow

class LocationLocalDataSource(
    private val dao: LocationDao
): ILocationLocalDataSource {

    override fun getLocationsForGroup(groupId: String): Flow<List<BaseLocationModel>> {
        return LocationMapper.mapFlowOfList(dao.getLocationsForGroup(groupId))
    }

    override fun getLocationsForUser(userId: String): Flow<List<BaseLocationModel>> {
        return LocationMapper.mapFlowOfList(dao.getLocationsForUser(userId))
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
}