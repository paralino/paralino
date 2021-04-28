package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.locationtrack.data.mappers.GroupsMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class UserGroupLocalDataSource (
    private val dao: UserGroupDao
): IGroupLocalDataSource {

    override suspend fun updateOrCreateGroup(group: UserGroup) {
        dao.createOrUpdate(GroupsMapper.mapReversed(group))
    }

    override fun getGroup(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId).mapLatest { GroupsMapper.map(it) }
    }

    override fun getAllGroups(): Flow<List<UserGroup>> {
        return GroupsMapper.mapFlowOfList(dao.getAllGroups())
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
    }

    override suspend fun clearAll() {
        dao.clear()
    }

}