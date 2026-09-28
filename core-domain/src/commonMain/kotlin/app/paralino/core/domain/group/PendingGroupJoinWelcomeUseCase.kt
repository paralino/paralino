package app.paralino.core.domain.group

import app.paralino.core.models.group.PendingGroupJoinWelcome
import app.paralino.core.repo.group.PendingGroupJoinWelcomeLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PendingGroupJoinWelcomeUseCase(
    private val local: PendingGroupJoinWelcomeLocalDataSource
) {
    fun observeAll(): Flow<List<PendingGroupJoinWelcome>> {
        return local.observeAll()
    }
    
    fun observeFirstUnshownGroupId(): Flow<String?> {
        return local.observeAll()
            .map { pending -> pending.firstOrNull { !it.didShowWelcome }?.groupId }
            .distinctUntilChanged()
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
