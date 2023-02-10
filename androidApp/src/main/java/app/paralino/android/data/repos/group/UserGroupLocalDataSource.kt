package app.paralino.android.data.repos.group

import app.paralino.android.data.mappers.GroupsMapperLocal
import app.paralino.core.config
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.repo.group.IGroupLocalDataSource
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.*

class UserGroupLocalDataSource constructor(
    private val dao: UserGroupDao,
    private val mapper: GroupsMapperLocal,
    private val memberLocal: ILocationLocalDataSource
) : IGroupLocalDataSource {

    override suspend fun recreateGroups(data: List<GroupWithMembersRaw>) {
        dao.replaceGroups(*data.map { mapper.remote2Local(it.group) }.toTypedArray())
        memberLocal.recreateAllMembers(data.flatMap { it.members })
    }

    override suspend fun updateOrCreateGroup(group: GroupWithMembersRaw) {
        dao.createOrUpdate(mapper.remote2Local(group.group))
        memberLocal.recreateMembersForGroup(group.group.id, group.members)
    }

    override suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        dao.createOrUpdate(mapper.remote2Local(group))
    }

    // TODO if group is deleted, changes are not propagated because of filterNotNull()

    override fun getGroupRawPresentation(groupId: String): Flow<Pair<GroupDocumentRaw, UserGroup>> {
        return dao.getGroup(groupId)
            .filterNotNull().let {
                combine(it, memberLocal.getMemberCountInGroup(groupId), memberLocal.getMember(groupId, config.userId)) { group, memberCount, member ->
                    mapper.local2Remote(group) to mapper.local2Core(group, memberCount, member.member.verificationResult)
                }
            }
    }

    override fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return dao.getGroup(groupId)
            .filterNotNull().let {
                combine(it, memberLocal.getMemberCountInGroup(groupId), memberLocal.getMember(groupId, config.userId)) { group, memberCount, member ->
                    mapper.local2Core(group, memberCount, member.member.verificationResult)
                }
            }
    }

    override fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers> {
        return dao.getGroup(groupId)
            .combine(memberLocal.getMembersForGroup(groupId)) { group, members ->
                if(group != null) {
                    val verificationResult =
                        members.first { it.member.userId == config.userId }.member.verificationResult
                    UserGroupWithMembers(
                        group = mapper.local2Core(group, members.size, verificationResult),
                        members = members
                    )
                } else null
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
                        it.map {
                            val verificationResult = memberLocal.getMemberOnce(it.first.id, config.userId)?.member?.verificationResult
                                ?: VerificationResult.GENERIC_ERROR
                            mapper.local2Core(it.first, it.second, verificationResult)
                        }
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