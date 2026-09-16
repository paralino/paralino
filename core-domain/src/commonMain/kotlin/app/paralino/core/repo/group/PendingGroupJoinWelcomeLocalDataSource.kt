package app.paralino.core.repo.group

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.groups.PendingGroupJoinWelcomeEntity
import app.paralino.core.models.group.PendingGroupJoinWelcome
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendingGroupJoinWelcomeLocalDataSource(
    private val database: MainDatabaseHelper
) {

    fun observeAll(): Flow<List<PendingGroupJoinWelcome>> {
        return database.pendingGroupJoinWelcomeDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveIfAbsent(groupId: String) {
        database.pendingGroupJoinWelcomeDao.insert(
            PendingGroupJoinWelcomeEntity(
                groupId = groupId,
                didShowWelcome = false,
                createdAtMillis = nowUTC().toEpochMilliseconds()
            )
        )
    }

    suspend fun markShown(groupId: String) {
        database.pendingGroupJoinWelcomeDao.markShown(groupId = groupId)
    }

    suspend fun delete(groupId: String) {
        database.pendingGroupJoinWelcomeDao.delete(groupId = groupId)
    }

    private fun PendingGroupJoinWelcomeEntity.toDomain(): PendingGroupJoinWelcome {
        return PendingGroupJoinWelcome(
            groupId = groupId,
            didShowWelcome = didShowWelcome,
            createdAtMillis = createdAtMillis
        )
    }
}
