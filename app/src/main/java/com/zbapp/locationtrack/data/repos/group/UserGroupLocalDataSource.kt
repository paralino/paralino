package com.zbapp.locationtrack.data.repos.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupWithMembers
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.location.ILocationLocalDataSource
import com.zbapp.core.utils.extensions.distinctFlowOfList
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
            .filterNotNull()
            .map { mapper.local2Core(it, memberLocal.memberCountInGroup(groupId)) }
    }

    override fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers> {
        return dao.getGroup(groupId)
            .combine(memberLocal.getMembersForGroup(groupId)) { group, members ->
                if(group != null)
                    UserGroupWithMembers(
                        group = mapper.local2Core(group, members.size),
                        members = members
                    )
                else null
            }
            .filterNotNull()
    }

    override fun getAllGroups(): Flow<List<UserGroup>> {
        return dao.getAllGroups()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Core(it, memberLocal.memberCountInGroup(it.id)) } }
            .map { it.sortedBy { it.name } }
    }

    override fun getAllGroupIds(): Flow<List<String>> {
        return dao.getAllGroupIds()
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