package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.raw.MemberDocumentRaw
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.mappers.MemberMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class LocationLocalDataSource(
    private val dao: MemberDao,
    private val mapper: MemberMapper
): ILocationLocalDataSource {

    override suspend fun addLocation(data: MemberDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(data))
    }

    override suspend fun addLocations(data: List<MemberDocumentRaw>) {
        dao.saveOnlyOneLocation(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override fun getLocationForUser(userId: String): Flow<MemberDocumentRaw> {
        return dao.getLastLocationForUser(userId).mapLatest(mapper::local2Remote)
    }

    override fun getLocationsForUsers(userIds: List<String>): Flow<List<MemberDocumentRaw>> {
        return mapper.mapFlowOfList(dao.getLocationsForUsers(userIds))
    }

    override suspend fun clearForUser(userId: String) {
        dao.clearForUser(userId)
    }

    override suspend fun clearAll() {
        dao.clear()
    }
}