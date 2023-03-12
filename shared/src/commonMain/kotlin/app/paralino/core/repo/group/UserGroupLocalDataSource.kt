package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.*

class UserGroupLocalDataSource constructor(
    private val database: AppDatabaseHelper,
    private val mapper: GroupsMapperLocal,
    private val memberMapper: MemberWithLocationsMapperLocal,
    private val memberLocal: LocationLocalDataSource
) {

    suspend fun recreateGroups(data: List<GroupWithMembersRaw>) {
        database.transaction {
            groupQueries.clearAll()
            memberQueries.clearAll()
            locationQueries.clearAll()
            data.forEach {
                groupQueries.createGroup(mapper.remote2Local(it.group))
                it.members.forEach { member ->
                    memberQueries.create(memberMapper.remote2Local(member.member))
                    member.locations.forEach {
                        locationQueries.create(memberMapper.remote2Local(member.member, it))
                    }
                }
            }
        }
    }

    suspend fun updateOrCreateGroup(group: GroupWithMembersRaw) {
        database.transaction {
            groupQueries.createGroup(mapper.remote2Local(group.group))
            memberQueries.clearForGroup(groupId = group.group.id)
            locationQueries.clearForGroup(groupId = group.group.id)
            group.members.forEach { member ->
                memberQueries.create(memberMapper.remote2Local(member.member))
                member.locations.forEach {
                    locationQueries.create(memberMapper.remote2Local(member.member, it))
                }
            }
        }
    }

    suspend fun updateOrCreateMember(member: MemberWithLocationsRaw) {
        database.transaction {
            memberQueries.create(memberMapper.remote2Local(member.member))
            locationQueries.clearForMember(member.member.id)
            member.locations.forEach {
                locationQueries.create(memberMapper.remote2Local(member.member, it))
            }
        }
    }

    suspend fun updateGroupOnly(group: GroupDocumentRaw) {
        database.groupQueries.createGroup(mapper.remote2Local(group))
    }

    // TODO if group is deleted, changes are not propagated because of filterNotNull()

    fun getGroupRawPresentation(groupId: String): Flow<Pair<GroupDocumentRaw, UserGroup>> {
        return database.mapToOneOrNull { groupQueries.getGroup(groupId) }
            .filterNotNull()
            .let {
                combine(it, memberLocal.getMemberCountInGroup(groupId), memberLocal.getMember(groupId, config.userId)) { group, memberCount, member ->
                    mapper.local2Remote(group) to mapper.local2Core(group, memberCount, member.verificationResult)
                }
            }
    }

    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return database.mapToOneOrNull { groupQueries.getGroup(groupId) }
            .filterNotNull().let {
                combine(it, memberLocal.getMemberCountInGroup(groupId), memberLocal.getMember(groupId, config.userId)) { group, memberCount, member ->
                    mapper.local2Core(group, memberCount, member.verificationResult)
                }
            }
    }

    fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers> {
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

    fun getAllGroups(): Flow<List<UserGroup>> {
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

    fun getAllGroupIds(): Flow<List<String>> {
        return database.mapToList { groupQueries.getAllGroupIds() }
    }

    suspend fun getAllGroupsCount(): Int {
        return database.groupQueries.getGroupsCount().executeAsOneOrNull()?.toInt() ?: 0
    }

    suspend fun deleteGroup(groupId: String) {
        database.groupQueries.deleteGroup(groupId)
        memberLocal.removeAllMembersForGroup(groupId)
    }

    suspend fun removeMember(groupId: String, userId: String) {
        memberLocal.removeMemberFromGroup(groupId, userId)
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupQueries.getGroupOwnerId(groupId).executeAsOne()
    }

    suspend fun clearAll() {
        database.groupQueries.clearAll()
        memberLocal.clearAll()
    }

}