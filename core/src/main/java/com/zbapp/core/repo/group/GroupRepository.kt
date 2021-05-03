package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class GroupRepository(
    private val local: IGroupLocalDataSource,
    private val remote: IGroupRemoteDataSource
): BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return local.getAllGroups()
    }

    fun getMyGroups(): Flow<List<UserGroup>> {
        return getAllGroups().mapLatest { it.filter { it.isFromUser } }
    }

    fun getGroup(id: String): Flow<UserGroup?> {
        return subscribeToData(
            local = local.getGroup(id),
            remote = { remote.getGroup(id) },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.PREFER
        )
    }

    fun createOrUpdateGroup(userGroup: UserGroup): Flow<UserGroup> {
        return getDataOnce(
            remote = { remote.createOrUpdateGroup(userGroup) },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun deleteGroup(id: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deleteGroup(id) },
            localUpdate = { local.deleteGroup(id) },
            remotePreference = RemotePreference.FORCE
        )
    }

    suspend fun clear() {
        local.clearAll()
    }

}