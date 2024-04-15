package app.paralino.core.repo.location

import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.logg
import app.paralino.core.models.location.MemberLocation
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
) {

    suspend fun createOrUpdateMembersWithoutSettings(vararg data: MemberDocumentRaw) {
        database.memberQueries.transaction {
            data.forEach {
                val currentMember = database.memberQueries.getMemberById(it.id).executeAsOneOrNull()
                val newMember = mapper.remote2Local(it).copy(
                    settings_id = currentMember?.settings_id,
                    settings_groupId = currentMember?.settings_groupId,
                    settings_ownerUserId = currentMember?.settings_ownerUserId,
                    settings_integrity = currentMember?.settings_integrity,
                )
                database.memberQueries.create(newMember)
            }
        }
    }

    suspend fun createOrUpdateMemberLocation(data: MemberLocationRaw) {
        logg.v("Save new location (${data.id})")
        database.locationQueries.update(
            id = data.id,
            deviceId = data.deviceId,
            deviceData = data.deviceData,
            groupKeyVersion = data.groupKeyVersion,
            deviceKeyVersion = data.deviceKeyVersion,
            locationData = data.locationData,
            status = data.status
        )
    }

    fun getMemberRawPresentation(memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>> {
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

    suspend fun getMemberModelsForUser(userId: String): List<MemberModel> {
        return database.memberQueries.getAllMembersForUser(userId).executeAsList().map {
            val ownerId = getGroupOwnerId(groupId = it.groupId)!!
            mapper.local2Core(it, emptyList(), ownerId).member
        }
    }

    fun getMembersForGroup(groupId: String): Flow<List<MemberModelWithLocations>> {
        return combine(
            database.mapToList { memberQueries.getAllMembersForGroup(groupId) },
            database.mapToList { locationQueries.getForGroup(groupId) }
        ) { members, locations ->
            members.map { member ->
                val ownerId = getGroupOwnerId(groupId = member.groupId)!!
                val memberLocations = locations.filter {
                    it.memberId == member.id
                }
                mapper.local2Core(member, memberLocations, ownerId)
            }
        }
            .distinctFlowOfList()
    }

    fun getMember(groupId: String, userId: String): Flow<MemberModel> {
        return database.mapToOneOrNull { memberQueries.getMember(groupId = groupId, userId = userId) }
            .filterNotNull()
            .map {
                val ownerId = getGroupOwnerId(it.groupId)!!
                mapper.local2Core(it, emptyList(), ownerId).member
            }
    }

    suspend fun getMemberOnce(groupId: String, userId: String): MemberModel? {
        return database.memberQueries.getMember(
            groupId = groupId,
            userId = userId
        ).executeAsOneOrNull()?.let {
            val ownerId = getGroupOwnerId(it.groupId)!!
            mapper.local2Core(it, emptyList(), ownerId).member
        }
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
        val ownerId = getGroupOwnerId(groupId)!!
        return mapper.local2Core(result.first, result.second, ownerId)
    }

    fun getLocationCountForMember(memberId: String): Flow<Int> {
        return database.mapToOneOrNull { locationQueries.getLocationCountForMember(memberId) }
            .mapNotNull { it?.toInt() }
            .distinctUntilChanged()
    }

    fun getMemberCountInGroup(groupId: String): Flow<Int> {
        return database.mapToOneOrNull { memberQueries.getMemberCount(groupId) }
            .mapNotNull { it?.toInt() }
            .distinctUntilChanged()
    }

    suspend fun getMemberCountInGroupOnce(groupId: String): Int {
        return database.memberQueries.getMemberCount(groupId).executeAsOneOrNull()?.toInt() ?: 0
    }

    suspend fun getUserMemberId(groupId: String, userId: String): String? {
        return database.memberQueries.getUserMemberId(groupId = groupId, userId = userId).executeAsOneOrNull()
    }

    suspend fun getMemberModelsWithLocationForUser(userId: String): List<MemberModelWithLocations> {
        val result = database.transactionWithResult {
            val members = database.memberQueries.getAllMembersForUser(userId = userId).executeAsList()
            val locations = database.locationQueries.getForUser(userId = userId).executeAsList()
            members to locations
        }

        return result?.first?.map { member ->
            val ownerId = getGroupOwnerId(groupId = member.groupId)!!
            val memberLocations = result.second.filter {
                it.memberId == member.id
            }
            mapper.local2Core(member, memberLocations, ownerId)
        } ?: emptyList()
    }

    suspend fun clearMemberLocationData(locationId: String) {
        database.locationQueries.updateLocationData(locationData = null, id = locationId)
    }

    suspend fun removeAllMembersForGroup(groupId: String) {
        database.transaction {
            memberQueries.clearForGroup(groupId)
            locationQueries.clearForGroup(groupId)
        }
    }

    suspend fun removeMemberFromGroup(groupId: String, userId: String) {
        database.transaction {
            memberQueries.deleteMember(groupId = groupId, userId = userId)
            locationQueries.clearForUserInGroup(groupId = groupId, userId = userId)
        }
    }

    private suspend fun getGroupOwnerId(groupId: String): String? {
        return database.groupQueries.getGroupOwnerId(groupId).executeAsOneOrNull()
    }
}