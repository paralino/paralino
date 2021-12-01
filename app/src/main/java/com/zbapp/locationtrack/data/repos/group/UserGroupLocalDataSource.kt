package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.locationtrack.data.mappers.GroupsMapper
import com.zbapp.locationtrack.data.mappers.mapFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class UserGroupLocalDataSource(
    private val dao: UserGroupDao,
    private val mapper: GroupsMapper,
    private val memberLocal: ILocationLocalDataSource
) : IGroupLocalDataSource {

    override suspend fun recreateGroups(data: List<GroupDocumentRaw>) {
        dao.replaceGroups(*data.map { mapper.remote2Local(it) }.toTypedArray())
        memberLocal.recreateAllMember(data.flatMap { it.members })
    }

    override suspend fun updateOrCreateGroup(group: GroupDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(group))
        memberLocal.recreateMembersForGroup(group.id, group.members)
    }

    override fun getGroup(groupId: String): Flow<GroupDocumentRaw> {
        return dao.getGroup(groupId)
            .combine(memberLocal.getMembersForGroup(groupId)) { group, members ->
                mapper.local2Remote(group).copy(members = members)
            }
    }

    override fun getAllGroups(): Flow<List<GroupDocumentRaw>> {
        return mapper.mapFlowOfList(dao.getAllGroups())
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
        memberLocal.clearForGroup(groupId)
    }

    override suspend fun clearAll() {
        dao.clear()
        memberLocal.clearAll()
    }

}