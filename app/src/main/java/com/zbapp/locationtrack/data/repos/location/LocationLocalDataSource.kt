package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.mappers.LocationMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class LocationLocalDataSource(
    private val dao: LocationDao,
    private val mapper: LocationMapper
): ILocationLocalDataSource {

    override suspend fun addLocation(data: SimpleDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(data))
    }

    override suspend fun addLocations(data: List<SimpleDocumentRaw>) {
        dao.saveOnlyOneLocation(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override fun getLocationForUser(userId: String): Flow<SimpleDocumentRaw> {
        return dao.getLastLocationForUser(userId).mapLatest(mapper::local2Remote)
    }

    override fun getLocationsForUsers(userIds: List<String>): Flow<List<SimpleDocumentRaw>> {
        return mapper.mapFlowOfList(dao.getLocationsForUsers(userIds))
    }

    override suspend fun clearForUser(userId: String) {
        dao.clearForUser(userId)
    }

    override suspend fun clearAll() {
        dao.clear()
    }
}