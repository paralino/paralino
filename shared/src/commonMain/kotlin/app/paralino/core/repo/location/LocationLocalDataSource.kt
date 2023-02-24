package app.paralino.core.repo.location

import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.*

class LocationLocalDataSource(
    private val database: AppDatabaseHelper,
    private val mapper: MemberWithLocationsMapperLocal
) : ILocationLocalDataSource {

    override suspend fun createOrUpdateMembersWithSettings(vararg data: MemberDocumentRaw) {
        database.memberQueries.transaction {
            data.forEach {
                database.memberQueries.create(mapper.remote2Local(it))
            }
        }
    }

    override suspend fun createOrUpdateMembersWithoutSettings(vararg data: MemberDocumentRaw) {
        database.memberQueries.transaction {
            data.forEach {
                val currentMember = database.memberQueries.getMemberById(it.id).executeAsOneOrNull()
                val newMember = mapper.remote2Local(it).copy(
                    settings_id = currentMember?.settings_id,
                    settings_groupId = currentMember?.settings_groupId,
                    settings_ownerUserId = currentMember?.settings_ownerUserId,
                    settings_secretKey = currentMember?.settings_secretKey,
                    settings_integrity = currentMember?.settings_integrity,
                )
                database.memberQueries.create(newMember)
            }
        }
    }

    override suspend fun createOrUpdateMemberLocation(data: MemberLocationRaw) {
        database.locationQueries.update(
            id = data.id,
            deviceId = data.deviceId,
            deviceData = data.deviceData,
            locationData = data.locationData
        )
    }

    override suspend fun recreateMembers(vararg data: MemberWithLocationsRaw) {
        database.transaction {
            data.forEach { member ->
                memberQueries.create(mapper.remote2Local(member.member))
                locationQueries.clearForMember(member.member.id)
                member.locations.forEach {
                    locationQueries.create(mapper.remote2Local(member.member, it))
                }
            }
        }
    }

    override suspend fun recreateAllMembers(data: List<MemberWithLocationsRaw>) {
        database.transaction {
            memberQueries.clearAll()
            locationQueries.clearAll()
            data.forEach { member ->
                memberQueries.create(mapper.remote2Local(member.member))
                member.locations.forEach {
                    locationQueries.create(mapper.remote2Local(member.member, it))
                }
            }
        }
    }

    override suspend fun recreateMembersForGroup(
        groupId: String,
        data: List<MemberWithLocationsRaw>
    ) {
        database.transaction {
            memberQueries.clearForGroup(groupId)
            locationQueries.clearForGroup(groupId)
            data.forEach { member ->
                memberQueries.create(mapper.remote2Local(member.member))
                member.locations.forEach {
                    locationQueries.create(mapper.remote2Local(member.member, it))
                }
            }
        }
    }

    override fun getMemberRawPresentation(memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>> {
        return combine(
            database.mapToOneOrNull { memberQueries.getMemberById(memberId) }.filterNotNull(),
            database.mapToList { locationQueries.getForMember(memberId) }
        ) { member, locations ->
            val ownerId = getGroupOwnerId(groupId = member.groupId)!!
            val raw = mapper.local2Remote(member, locations)
            val core = mapper.local2Core(member, locations, ownerId)
            raw to core
        }
    }

    override suspend fun getMemberModelsForUser(userId: String): List<MemberModel> {
        return database.memberQueries.getAllMembersForUser(userId).executeAsList().map {
            val ownerId = getGroupOwnerId(groupId = it.groupId)!!
            mapper.local2Core(it, emptyList(), ownerId).member
        }
    }

    override fun getMembersForGroup(groupId: String): Flow<List<MemberModelWithLocations>> {
        return combine(
            database.mapToList { memberQueries.getAllMembersForGroup(groupId) },
            database.mapToList { locationQueries.getForGroup(groupId) }
        ) { members, locations ->
            members.map { member ->
                val ownerId = getGroupOwnerId(groupId = member.groupId)!!
                mapper.local2Core(member, locations, ownerId)
            }
        }
            .distinctFlowOfList()
    }

    override fun getMember(groupId: String, userId: String): Flow<MemberModel> {
        return database.mapToOneOrNull { memberQueries.getMember(groupId = groupId, userId = userId) }
            .filterNotNull()
            .map {
                val ownerId = getGroupOwnerId(it.groupId)!!
                mapper.local2Core(it, emptyList(), ownerId).member
            }
    }

    override suspend fun getMemberOnce(groupId: String, userId: String): MemberModel? {
        return database.memberQueries.getMember(
            groupId = groupId,
            userId = userId
        ).executeAsOneOrNull()?.let {
            val ownerId = getGroupOwnerId(it.groupId)!!
            mapper.local2Core(it, emptyList(), ownerId).member
        }
    }

    override suspend fun getMemberWithLocationsOnce(
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
        val ownerId = getGroupOwnerId(groupId)!!
        return mapper.local2Core(result.first, result.second, ownerId)
    }

    override fun getMemberCountInGroup(groupId: String): Flow<Int> {
        return database.mapToOneOrNull { memberQueries.getMemberCount(groupId) }
            .mapNotNull { it?.toInt() }
            .distinctUntilChanged()
    }

    override suspend fun getMemberCountInGroupOnce(groupId: String): Int {
        return database.memberQueries.getMemberCount(groupId).executeAsOneOrNull()?.toInt() ?: 0
    }

    override suspend fun getUserMemberId(groupId: String, userId: String): String? {
        return database.memberQueries.getUserMemberId(groupId = groupId, userId = userId).executeAsOneOrNull()
    }

    override suspend fun clearMemberLocationData(locationId: String) {
        database.locationQueries.updateLocationData(locationData = null, id = locationId)
    }

    override suspend fun removeAllMembersForGroup(groupId: String) {
        database.transaction {
            memberQueries.clearForGroup(groupId)
            locationQueries.clearForGroup(groupId)
        }
    }

    override suspend fun removeMemberFromGroup(groupId: String, userId: String) {
        database.transaction {
            memberQueries.deleteMember(groupId = groupId, userId = userId)
            locationQueries.clearForUserInGroup(groupId = groupId, userId = userId)
        }
    }

    override suspend fun clearAll() {
        database.transaction {
            memberQueries.clearAll()
            locationQueries.clearAll()
        }
    }

    private suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupQueries.getGroupOwnerId(groupId).executeAsOne()
    }
}