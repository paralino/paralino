package app.paralino.core.domain.privacyzone

import app.paralino.core.domain.location.PrivacyZoneSharedLocationLocalSource
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.privacyzones.PrivacyZonesRemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DeletePrivacyZoneUseCase(
    private val remote: PrivacyZonesRemoteSource,
    private val groupLocal: UserGroupLocalDataSource,
    private val sharedLocationLocal: PrivacyZoneSharedLocationLocalSource,
) {
    fun invoke(privacyZoneId: String, groupId: String): Flow<Unit> {
        return flow {
            remote.deletePrivacyZone(privacyZoneId)
            deletePrivacyZoneLocally(groupId = groupId, privacyZoneId = privacyZoneId)
            emit(Unit)
        }
    }

    suspend fun deletePrivacyZoneLocally(groupId: String, privacyZoneId: String) {
        groupLocal.deletePrivacyZone(privacyZoneId)
        sharedLocationLocal.delete(groupId = groupId, privacyZoneId = privacyZoneId)
    }
}
