package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.locationtrack.data.mappers.GroupsMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import com.zbapp.locationtrack.data.models.group.UserGroupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class UserGroupLocalDataSource (
    private val dao: UserGroupDao,
    private val mapper: GroupsMapper
): IGroupLocalDataSource {

    override suspend fun replaceGroups(data: List<UserGroup>) {
        dao.replaceGroups(*data.map { mapper.core2Local(it) }.toTypedArray())
    }

    suspend fun updateOrCreateGroup(group: UserGroupEntity) {
        dao.createOrUpdate(group)
    }

    override suspend fun updateOrCreateGroup(group: UserGroup) {
        dao.createOrUpdate(mapper.core2Local(group))
    }

    override fun getGroup(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId).mapLatest { mapper.local2Core(it) }
    }

    override fun getAllGroups(): Flow<List<UserGroup>> {
        return mapper.mapFlowOfList(dao.getAllGroups())
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
    }

    override suspend fun clearAll() {
        dao.clear()
    }

}