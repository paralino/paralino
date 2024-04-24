package app.paralino.core.repo.group

import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.repo.base.BaseRepository
import kotlinx.coroutines.flow.Flow

class GroupRepository constructor(
    private val local: UserGroupLocalDataSource,
) : BaseRepository() {

    fun getAllGroupsLocally(): Flow<List<UserGroupWithMetadata>> {
        return local.getAllGroupsWithMetadata()
    }

    fun getGroupWithMembersLocally(id: String): Flow<UserGroupWithMembers?> {
        return local.getGroupWithMembers(id)
    }

    suspend fun clear() {
        local.clearAll()
    }
}