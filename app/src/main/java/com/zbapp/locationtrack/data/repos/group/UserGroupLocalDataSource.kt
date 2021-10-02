package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.raw.SimpleDocumentRaw
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.locationtrack.data.mappers.GroupsMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class UserGroupLocalDataSource (
    private val dao: UserGroupDao,
    private val mapper: GroupsMapper
): IGroupLocalDataSource {

    override suspend fun replaceGroups(data: List<SimpleDocumentRaw>) {
        dao.replaceGroups(*data.map { mapper.remote2Local(it) }.toTypedArray())
    }

    override suspend fun updateOrCreateGroup(group: SimpleDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(group))
    }

    override fun getGroup(groupId: String): Flow<SimpleDocumentRaw> {
        return dao.getGroup(groupId).mapLatest(mapper::local2Remote)
    }

    override fun getAllGroups(): Flow<List<SimpleDocumentRaw>> {
        return mapper.mapFlowOfList(dao.getAllGroups())
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
    }

    override suspend fun clearAll() {
        dao.clear()
    }

}