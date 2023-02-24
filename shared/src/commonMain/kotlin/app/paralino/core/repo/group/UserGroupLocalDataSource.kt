package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.*

class UserGroupLocalDataSource constructor(
    private val database: AppDatabaseHelper,
    private val mapper: GroupsMapperLocal,
    private val memberLocal: ILocationLocalDataSource
) : IGroupLocalDataSource {

    override suspend fun recreateGroups(data: List<GroupWithMembersRaw>) {
        database.groupQueries.transaction {
            database.groupQueries.clearAll()
            data.forEach {
                database.groupQueries.createGroup(mapper.remote2Local(it.group))
            }
        }
        memberLocal.recreateAllMembers(data.flatMap { it.members })
    }

    override suspend fun updateOrCreateGroup(group: GroupWithMembersRaw) {
        database.groupQueries.createGroup(mapper.remote2Local(group.group))
        memberLocal.recreateMembersForGroup(group.group.id, group.members)
    }

    override suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        database.groupQueries.createGroup(mapper.remote2Local(group))
    }

    // TODO if group is deleted, changes are not propagated because of filterNotNull()

    override fun getGroupRawPresentation(groupId: String): Flow<Pair<GroupDocumentRaw, UserGroup>> {
        return database.mapToOneOrNull { groupQueries.getGroup(groupId) }
            .filterNotNull()
            .let {
                combine(it, memberLocal.getMemberCountInGroup(groupId), memberLocal.getMember(groupId, config.userId)) { group, memberCount, member ->
                    mapper.local2Remote(group) to mapper.local2Core(group, memberCount, member.verificationResult)
                }
            }
    }

    override fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return database.mapToOneOrNull { groupQueries.getGroup(groupId) }
            .filterNotNull().let {
                combine(it, memberLocal.getMemberCountInGroup(groupId), memberLocal.getMember(groupId, config.userId)) { group, memberCount, member ->
                    mapper.local2Core(group, memberCount, member.verificationResult)
                }
            }
    }

    override fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers> {
        return database.mapToOneOrNull { groupQueries.getGroup(groupId) }
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
        return database.mapToList { groupQueries.getAllGroups() }
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
                            val verificationResult = memberLocal.getMemberOnce(it.first.id, config.userId)?.verificationResult
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
        return database.mapToList { groupQueries.getAllGroupIds() }
    }

    override suspend fun deleteGroup(groupId: String) {
        database.groupQueries.deleteGroup(groupId)
        memberLocal.removeAllMembersForGroup(groupId)
    }

    override suspend fun removeMember(groupId: String, userId: String) {
        memberLocal.removeMemberFromGroup(groupId, userId)
    }

    override suspend fun clearAll() {
        database.groupQueries.clearAll()
        memberLocal.clearAll()
    }

}