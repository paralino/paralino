package app.paralino.core.repo.location

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.location.ApproximateSharedLocationLocalSource
import app.paralino.core.db.main.groups.MemberEntityBatterySettingsUpdate
import app.paralino.core.db.main.groups.MemberLocationEntityDataUpdate
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.mappers.MemberWithLocationsMapperLocal
import app.paralino.core.models.raw.GroupSettingsRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

class LocationLocalDataSource(
    private val database: MainDatabaseHelper,
    private val mapper: MemberWithLocationsMapperLocal,
    private val approximateSharedLocationLocalSource: ApproximateSharedLocationLocalSource,
) {

    suspend fun createOrUpdateMembersWithoutSettings(vararg data: MemberDocumentRaw) {
        database.groupRelatedDao.createOrUpdateMembersWithoutSettings(
            data.map { mapper.remote2Local(it) }
        )
    }

    suspend fun updateMemberBatterySettings(data: GroupSettingsRaw) {
        logg.v("Save new member battery settings to database (${data.id})")
        database.groupRelatedDao.updateMemberBatterySettings(
            MemberEntityBatterySettingsUpdate(
                id = data.id,
                settingsBatteryAlertSettings = data.batteryAlertSettings
            )
        )
    }

    suspend fun getMemberOnce(group: UserGroup, userId: String): MemberModel? {
        return database.groupRelatedDao.getMemberOnce(groupId = group.id, userId = userId)?.let {
            mapper.local2Core(it, emptyList(), group).member
        }
    }

    fun getMemberCountInGroup(groupId: String): Flow<Int> {
        return database.groupRelatedDao.getMemberCount(groupId = groupId)
            .filterNotNull()
            .distinctUntilChanged()
    }

    suspend fun getUserMemberId(groupId: String, userId: String): String? {
        return database.groupRelatedDao.getUserMemberId(groupId = groupId, userId = userId)
    }

    suspend fun clearMemberLocationData(groupOrSpaceId: GroupOrSpaceId, locationId: String) {
        approximateSharedLocationLocalSource.delete(groupId = groupOrSpaceId)
        if (groupOrSpaceId.isPersonalSpace) {
            database.userDeviceDao.clearLocationDataForDevice(locationId = locationId)
        } else {
            database.groupRelatedDao.clearLocationDataForLocation(locationId = locationId)
        }
    }
}