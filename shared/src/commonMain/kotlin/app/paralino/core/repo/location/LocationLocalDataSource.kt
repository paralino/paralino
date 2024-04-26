package app.paralino.core.repo.location

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.logg
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

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
        logg.v("Save new location to database (${data.id})")
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

    suspend fun getMemberOnce(group: UserGroup, userId: String): MemberModel? {
        return database.memberQueries.getMember(
            groupId = group.id,
            userId = userId
        ).executeAsOneOrNull()?.let {
            mapper.local2Core(it, emptyList(), group).member
        }
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

    suspend fun clearMemberLocationData(locationId: String) {
        database.locationQueries.updateLocationData(locationData = null, id = locationId)
    }
}