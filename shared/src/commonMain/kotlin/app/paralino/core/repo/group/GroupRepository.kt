package app.paralino.core.repo.group

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.repo.base.BaseRepository
import kotlinx.coroutines.flow.Flow

class GroupRepository constructor(
    private val local: UserGroupLocalDataSource,
) : BaseRepository() {

    fun getAllGroupsLocally(): Flow<List<UserGroup>> {
        return local.getAllGroups()
    }

    fun getAllGroupIds(): Flow<List<String>> {
        return local.getAllGroupIds()
    }

    fun getGroupWithMembersLocally(id: String): Flow<UserGroupWithMembers> {
        return local.getGroupWithMembers(id)
    }

    fun getGroupWithoutMembersLocally(id: String): Flow<UserGroup> {
        return local.getGroupWithoutMembers(id)
    }

    suspend fun getAllGroupsCount(): Int {
        return local.getAllGroupsCount()
    }

    suspend fun clear() {
        local.clearAll()
    }

}