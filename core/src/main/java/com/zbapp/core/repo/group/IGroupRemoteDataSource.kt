package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup

interface IGroupRemoteDataSource {

    suspend fun initGroups()
    suspend fun createOrUpdateGroup(group: UserGroup): UserGroup
    suspend fun getAllGroups(): List<UserGroup>
    suspend fun getGroup(groupId: String): UserGroup
    suspend fun deleteGroup(groupId: String)

}