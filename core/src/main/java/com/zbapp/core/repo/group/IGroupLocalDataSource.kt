package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.GroupDocumentRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun recreateGroups(data: List<GroupDocumentRaw>)
    suspend fun updateOrCreateGroup(group: GroupDocumentRaw)

    fun getGroup(groupId: String): Flow<UserGroup>
    fun getAllGroups(): Flow<List<UserGroup>>

    suspend fun deleteGroup(groupId: String)
    suspend fun removeMember(memberId: String)
    suspend fun clearAll()

}