package app.paralino.core.domain.privacyzone

import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.models.raw.GroupPrivacyZoneRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesGetRemoteSource
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
import kotlinx.coroutines.flow.Flow

class GetPrivacyZonesUseCase(
    private val local: PrivacyZonesLocalDataSource,
    private val remoteGet: PrivacyZonesGetRemoteSource,
    private val groupLocal: UserGroupLocalDataSource
) : BaseRepository() {

    fun getPrivacyZonesForGroupLocally(groupId: String): Flow<List<GroupPrivacyZone>> {
        return local.getPrivacyZonesForGroup(groupId)
    }

    fun getAndRefreshPrivacyZonesForGroup(groupId: String): Flow<List<GroupPrivacyZone>> {
        return mergeDataWithRemote(
            local = local.getPrivacyZonesForGroup(groupId),
            remote = { remoteGet.getPrivacyZonesForGroup(groupId) },
            localUpdate = { groupLocal.recreatePrivacyZonesForGroup(groupId, it) },
        )
    }

    fun getPrivacyZoneLocally(zoneId: String): Flow<GroupPrivacyZone?> {
        return local.getPrivacyZone(zoneId)
    }

    suspend fun refreshPrivacyZoneMaybe(zoneId: String) {
        triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "privacy_zone_detail_$zoneId",
                timeMillis = 10_000,
            ),
        ) {
            remoteGet.getPrivacyZone(zoneId).also {
                groupLocal.updateOrCreatePrivacyZone(it)
            }
        }
    }

    suspend fun updatePrivacyZoneLocally(raw: GroupPrivacyZoneRaw) {
        groupLocal.updateOrCreatePrivacyZone(raw)
    }
}
