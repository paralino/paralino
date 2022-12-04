package app.paralino.android.data.repos.group

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.repo.group.IGroupLocalDataSource
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import app.paralino.android.data.mappers.GroupsMapperLocal
import kotlinx.coroutines.flow.*

class UserGroupLocalDataSource constructor(
    private val dao: UserGroupDao,
    private val mapper: GroupsMapperLocal,
    private val memberLocal: ILocationLocalDataSource
) : IGroupLocalDataSource {

    override suspend fun recreateGroups(data: List<GroupWithMemberRaw>) {
        dao.replaceGroups(*data.map { mapper.remote2Local(it.group) }.toTypedArray())
        memberLocal.recreateAllMembers(data.flatMap { it.members })
    }

    override suspend fun updateOrCreateGroup(group: GroupWithMemberRaw) {
        dao.createOrUpdate(mapper.remote2Local(group.group))
        memberLocal.recreateMembersForGroup(group.group.id, group.members)
    }

    override suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(group))
    }

    override fun getGroupRawPresentation(groupId: String): Flow<Pair<GroupDocumentRaw, UserGroup>> {
        return dao.getGroup(groupId)
            .filterNotNull()
            .combine(memberLocal.getMemberCountInGroup(groupId)) { group, memberCount ->
                mapper.local2Remote(group) to mapper.local2Core(group, memberCount)
            }
    }

    override fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId)
            .filterNotNull()
            .combine(memberLocal.getMemberCountInGroup(groupId)) { group, memberCount ->
                mapper.local2Core(group, memberCount)
            }
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
            .flatMapLatest { groups ->
                if(groups.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = groups.map { group ->
                        memberLocal.getMemberCountInGroup(group.id)
                            .map { group to it }
                    }
                    combine(flows) {
                        it.map { mapper.local2Core(it.first, it.second) }
                    }
                }
            }
            .distinctFlowOfList()
            .map { it.sortedBy { it.name } }
    }

    override fun getAllGroupIds(): Flow<List<String>> {
        return dao.getAllGroupIds()
    }

    override suspend fun deleteGroup(groupId: String) {
        dao.deleteGroup(groupId)
        memberLocal.removeAllMembersForGroup(groupId)
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        memberLocal.removeMemberFromGroup(groupId, userId)
    }

    override suspend fun clearAll() {
        dao.clear()
        memberLocal.clearAll()
    }

}