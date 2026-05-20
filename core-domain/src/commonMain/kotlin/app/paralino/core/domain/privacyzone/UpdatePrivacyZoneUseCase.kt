package app.paralino.core.domain.privacyzone

import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.domain.location.PrivacyZoneSharedLocationLocalSource
import app.paralino.core.repo.privacyzones.PrivacyZonesLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdatePrivacyZoneUseCase(
    private val remote: PrivacyZonesRemoteSource,
    private val local: PrivacyZonesLocalDataSource,
    private val sharedLocationLocal: PrivacyZoneSharedLocationLocalSource,
) {

    fun invoke(zone: GroupPrivacyZone): Flow<Unit> {
        return flow {
            val updated = remote.updatePrivacyZone(zone)
            sharedLocationLocal.delete(groupId = zone.groupId, privacyZoneId = zone.id)
            local.updatePrivacyZoneOnly(updated)
            emit(Unit)
        }
    }
}
