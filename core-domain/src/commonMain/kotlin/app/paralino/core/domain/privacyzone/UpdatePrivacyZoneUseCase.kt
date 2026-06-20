package app.paralino.core.domain.privacyzone

import app.paralino.core.models.privacyzones.GroupPrivacyZone
import app.paralino.core.domain.location.PrivacyZoneSharedLocationLocalSource
import app.paralino.core.models.data
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
            val localZone = local.getPrivacyZoneOnce(
                zoneId = zone.id
            )

            if (
                localZone?.data?.data?.radiusMeters != zone.data.data?.radiusMeters ||
                localZone?.data?.data?.lat != zone.data.data?.lat ||
                localZone?.data?.data?.lng != zone.data.data?.lng
            ) {
                // invalidated local location only if zone changes
                sharedLocationLocal.delete(groupId = zone.groupId, privacyZoneId = zone.id)
            }

            local.updatePrivacyZoneOnly(updated)
            emit(Unit)
        }
    }
}
