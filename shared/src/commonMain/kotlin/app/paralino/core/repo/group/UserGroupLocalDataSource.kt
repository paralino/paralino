package app.paralino.core.repo.group

import app.paralino.core.config
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.domain.crypto.VerificationResult
import app.paralino.core.domain.crypto.VerificationResultError
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.mappers.GroupsMapperLocal
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup> {
        return database.mapToOneOrNull { groupQueries.getGroup(groupId) }
            .filterNotNull()
            .map { mapper.local2Core(it) }
    }

    fun getGroupWithMetadata(groupId: String): Flow<UserGroupWithMetadata> {
        return combine(
            database.mapToOneOrNull { groupQueries.getGroup(groupId) }.filterNotNull(),
            memberLocal.getMemberCountInGroup(groupId),
            database.mapToOneOrNull { memberQueries.getMember(groupId = groupId, userId = config.userId) }.filterNotNull()
        ) { groupEntity, memberCount, member ->
            val group = mapper.local2Core(groupEntity)
            UserGroupWithMetadata(
                group = group,
                memberCount = memberCount,
                verification = memberMapper.local2Core(member, emptyList(), group).member.verificationResult
            )
        }
    }

    fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers?> {
        return combine(
            database.mapToOneOrNull { groupQueries.getGroup(groupId) }.distinctUntilChanged(),
            database.mapToList { memberQueries.getAllMembersForGroup(groupId) }.distinctFlowOfList(),
            database.mapToList { locationQueries.getForGroup(groupId) }.distinctFlowOfList()
        ) { groupEntity, members, locations ->
            if(groupEntity != null && members.isNotEmpty()) {
                // TODO double check this - sometimes it crashes on logout or expired session with NoSuchElement exception
                // mostly likely because clearing database currently is not one transaction

                val group = mapper.local2Core(groupEntity)
                val mappedMembers = members.map { member ->
                    val memberLocations = locations.filter { it.memberId == member.id }
                    memberMapper.local2Core(member, memberLocations, group)
                }

                UserGroupWithMembers(
                    group = mapper.local2Core(groupEntity),
                    members = mappedMembers
                )
            } else null
        }
    }

    fun getAllGroupsWithMetadata(): Flow<List<UserGroupWithMetadata>> {
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
                            val group = mapper.local2Core(it.first)
                            val verificationResult = memberLocal.getMemberOnce(group, config.userId)?.verificationResult
                                ?: VerificationResult.Unverified(errors = listOf(VerificationResultError.MEMBER_VERIFICATION_MISSING))
                            UserGroupWithMetadata(
                                group = group,
                                memberCount = it.second,
                                verification = verificationResult
                            )
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

    suspend fun getMemberModelsWithLocationForUser(userId: String): List<MemberModelWithLocations> {
        val result = database.transactionWithResult {
            val members = database.memberQueries.getAllMembersForUser(userId = userId).executeAsList()
            val locations = database.locationQueries.getForUser(userId = userId).executeAsList()
            members to locations
        }

        return result?.first?.map { member ->
            val group = getGroupOnce(member.groupId)!!
            val memberLocations = result.second.filter {
                it.memberId == member.id
            }
            memberMapper.local2Core(member, memberLocations, group)
        } ?: emptyList()
    }

    suspend fun getMemberWithLocationsOnce(
        groupId: String,
        userId: String
    ): MemberModelWithLocations? {
        val result = database.transactionWithResult {
            val member = memberQueries.getMember(
                groupId = groupId,
                userId = userId
            ).executeAsOneOrNull() ?: return@transactionWithResult null
            val locations = locationQueries.getForMember(memberId = member.id).executeAsList()
            member to locations
        } ?: return null
        val group = getGroupOnce(groupId)!!
        return memberMapper.local2Core(result.first, result.second, group)
    }


    fun getMemberRawPresentation(group: UserGroup?, memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>> {
        return combine(
            database.mapToOneOrNull { memberQueries.getMemberById(memberId) }.filterNotNull(),
            database.mapToList { locationQueries.getForMember(memberId) }
        ) { member, locations ->
            val group = group ?: getGroupOnce(member.groupId)!!
            val raw = memberMapper.local2Remote(member, locations)
            val core = memberMapper.local2Core(member, locations, group)
            raw to core
        }
    }

    suspend fun getMemberModelsForUser(userId: String): List<MemberModel> {
        return database.memberQueries.getAllMembersForUser(userId).executeAsList().map {
            val group = getGroupOnce(it.groupId)!!
            memberMapper.local2Core(it, emptyList(), group).member
        }
    }

    suspend fun getMemberOnce(groupId: String, userId: String): MemberModel? {
        return database.memberQueries.getMember(
            groupId = groupId,
            userId = userId
        ).executeAsOneOrNull()?.let {
            val group = getGroupOnce(it.groupId)!!
            memberMapper.local2Core(it, emptyList(), group).member
        }
    }

    suspend fun deleteGroup(groupId: String) {
        database.transaction {
            groupQueries.deleteGroup(groupId)
            memberQueries.clearForGroup(groupId)
            locationQueries.clearForGroup(groupId)
        }
    }

    suspend fun removeMember(groupId: String, userId: String) {
        database.transaction {
            memberQueries.deleteMember(groupId = groupId, userId = userId)
            locationQueries.clearForUserInGroup(groupId = groupId, userId = userId)
        }
    }

    suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupQueries.getGroupOwnerId(groupId).executeAsOne()
    }

    suspend fun getGroupOnce(groupId: String): UserGroup? {
        return database.groupQueries.getGroup(groupId).executeAsOneOrNull()
            ?.let { mapper.local2Core(it) }
    }

    suspend fun clearAll() {
        database.transaction {
            groupQueries.clearAll()
            memberQueries.clearAll()
            locationQueries.clearAll()
        }
    }
}