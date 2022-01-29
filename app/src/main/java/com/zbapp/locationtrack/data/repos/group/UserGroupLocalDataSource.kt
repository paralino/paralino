package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.utils.distinctFlowOfList
import com.zbapp.locationtrack.data.mappers.GroupsMapperLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class UserGroupLocalDataSource constructor(
    private val dao: UserGroupDao,
    private val mapper: GroupsMapperLocal,
    private val memberLocal: ILocationLocalDataSource
) : IGroupLocalDataSource {

    override suspend fun recreateGroups(data: List<GroupWithMemberRaw>) {
        dao.replaceGroups(*data.map { mapper.remote2Local(it.group) }.toTypedArray())
        memberLocal.recreateAllMember(data.flatMap { it.members })
    }

    override suspend fun updateOrCreateGroup(group: GroupWithMemberRaw) {
        dao.createOrUpdate(mapper.remote2Local(group.group))
        memberLocal.recreateMembersForGroup(group.group.id, group.members)
    }

    override suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(group))
    }

    override fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId)
            .map { mapper.local2Core(it) }
    }

    override fun getGroupWithMembers(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId)
            .filterNotNull()
            .combine(memberLocal.getMembersForGroup(groupId)) { group, members ->
                mapper.local2Core(group).copy(members = members)
            }
    }

    override fun getAllGroups(): Flow<List<UserGroup>> {
        return dao.getAllGroups()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Core(it) } }
            .map { it.sortedBy { it.name } }
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
        memberLocal.clearAllMembersForGroup(groupId)
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        memberLocal.clearAllMembersForGroup(groupId, userId)
    }

    override suspend fun clearAll() {
        dao.clear()
        memberLocal.clearAll()
    }

}