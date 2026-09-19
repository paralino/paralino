package app.paralino.core.domain.group

import app.paralino.core.models.group.PendingGroupJoinWelcome
import app.paralino.core.repo.group.PendingGroupJoinWelcomeLocalDataSource
import kotlinx.coroutines.flow.Flow

class PendingGroupJoinWelcomeUseCase(
    private val local: PendingGroupJoinWelcomeLocalDataSource
) {
    fun observeAll(): Flow<List<PendingGroupJoinWelcome>> {
        return local.observeAll()
    }

    suspend fun saveIfAbsent(groupId: String) {
        local.saveIfAbsent(groupId = groupId)
    }

    suspend fun markShown(groupId: String) {
        local.markShown(groupId = groupId)
    }

    suspend fun complete(groupId: String) {
        local.delete(groupId = groupId)
    }

    suspend fun delete(groupId: String) {
        local.delete(groupId = groupId)
    }
}
