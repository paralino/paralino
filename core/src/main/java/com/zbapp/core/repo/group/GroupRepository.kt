package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.mappers.IGroupDataMapperCore
import com.zbapp.core.models.mappers.decrypt
import com.zbapp.core.models.mappers.decryptList
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class GroupRepository(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val mapper: IGroupDataMapperCore
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return subscribeToData(
            local = local.getAllGroups().decryptList(mapper),
            remote = { remote.getAllGroups() },
            localUpdate = { local.replaceGroups(it) },
            remotePreference = RemotePreference.CACHE("groups", 10_000)
        )
    }

    fun getAllGroupsLocally(): Flow<List<UserGroup>> {
        return local.getAllGroups().decryptList(mapper)
    }

    fun getMyGroups(): Flow<List<UserGroup>> {
        return getAllGroups().mapLatest { it.filter { it.isFromUser } }
    }

    fun getGroup(id: String): Flow<UserGroup> {
        return subscribeToData(
            local = local.getGroup(id).decrypt(mapper),
            remote = { remote.getGroup(id) },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.PREFER
        )
    }

    fun createOrUpdateGroup(userGroup: UserGroup): Flow<Unit> {
        return getDataOnce(
            remote = { remote.createOrUpdateGroup(mapper.core2Remote(userGroup)) },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.FORCE
        ).map { Unit }
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