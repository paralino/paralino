package app.paralino.core.repo.privacyzones

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.data
import app.paralino.core.models.mappers.GroupPrivacyZoneMapperLocal
import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.raw.GroupPrivacyZoneRaw
import app.paralino.core.utils.extensions.distinctFlowOfList
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class PrivacyZonesLocalDataSource(
    private val database: MainDatabaseHelper,
    private val mapper: GroupPrivacyZoneMapperLocal,
    private val appPrefs: AppPrefs,
) {

    suspend fun updatePrivacyZoneOnly(zone: GroupPrivacyZoneRaw) {
        database.groupRelatedDao.createPrivacyZone(mapper.remote2Local(zone))
    }

    fun getPrivacyZoneRawPresentation(zoneId: String): Flow<Pair<GroupPrivacyZoneRaw, GroupPrivacyZone>> {
        return database.groupRelatedDao.getPrivacyZone(zoneId = zoneId)
            .filterNotNull()
            .distinctUntilChanged()
            .map { mapper.local2Remote(it) to mapper.local2Core(it) }
    }

    fun getPrivacyZone(zoneId: String): Flow<GroupPrivacyZone?> {
        return database.groupRelatedDao.getPrivacyZone(zoneId = zoneId)
            .distinctUntilChanged()
            .map { entity -> entity?.let { mapper.local2Core(it) } }
    }

    fun getPrivacyZonesForGroup(groupId: String): Flow<List<GroupPrivacyZone>> {
        return database.groupRelatedDao.getPrivacyZonesForGroup(
            groupId = groupId,
            ownerUserId = appPrefs.userId,
        )
            .distinctFlowOfList()
            .map { entities -> entities.map { mapper.local2Core(it) } }
    }

    suspend fun getPrivacyZoneOnce(zoneId: String): GroupPrivacyZone? {
        return database.groupRelatedDao.getPrivacyZoneOnce(zoneId = zoneId)
            ?.let { mapper.local2Core(it) }
    }

    suspend fun getNonExpiredPrivacyZonesForGroupOnce(groupId: String): List<GroupPrivacyZone> {
        val now = nowUTC()
        return database.groupRelatedDao.getNonExpiredPrivacyZonesForGroupOnce(
            groupId = groupId,
            ownerUserId = appPrefs.userId,
        )
            .map { mapper.local2Core(it) }
            .filter { it.data.data?.isExpired(now) != true }
    }

    suspend fun deletePrivacyZone(zoneId: String) {
        database.groupRelatedDao.deletePrivacyZone(zoneId = zoneId)
    }
}
