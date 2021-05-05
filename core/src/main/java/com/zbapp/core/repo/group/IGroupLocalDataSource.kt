package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun replaceGroups(data: List<UserGroup>)
    suspend fun updateOrCreateGroup(group: UserGroup)
    fun getGroup(groupId: String): Flow<UserGroup>
    fun getAllGroups(): Flow<List<UserGroup>>
    suspend fun deleteGroup(groupId: String)
    suspend fun clearAll()

}