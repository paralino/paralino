package com.zbapp.remote

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.repo.group.IGroupRemoteDataSource
import com.zbapp.remote.api.GroupsApi
import com.zbapp.remote.user.RemoteUserCache

class GroupsRemoteSource internal constructor(
    private val api: GroupsApi
): IGroupRemoteDataSource {

    constructor(user: RemoteUserCache) : this(GroupsApi(user))

    override suspend fun initGroups() {
        try { api.initGroups() }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun createOrUpdateGroup(group: UserGroup): UserGroup {
        try { return api.createOrUpdateGroup(group) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun getAllGroups(): List<UserGroup> {
        try { return api.getAllGroups() }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun getGroup(groupId: String): UserGroup {
        try { return api.getGroup(groupId) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

    override suspend fun deleteGroup(groupId: String) {
        try { api.deleteGroup(groupId) }
        catch (e: Exception) { throw ExceptionMapper.map(e) }
    }

}