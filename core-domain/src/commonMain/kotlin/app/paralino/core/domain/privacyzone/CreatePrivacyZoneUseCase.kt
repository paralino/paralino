package app.paralino.core.domain.privacyzone

import app.paralino.core.models.privacyzones.PrivacyZoneData
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CreatePrivacyZoneUseCase(
    private val remote: PrivacyZonesRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
) {

    fun invoke(
        groupId: String,
        data: PrivacyZoneData,
    ): Flow<Unit> {
        return flow {
            val zone = remote.createPrivacyZone(groupId = groupId, data = data)
            groupLocal.updateOrCreatePrivacyZone(zone)
            emit(Unit)
        }
    }
}
