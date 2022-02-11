package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun recreateGroups(data: List<GroupWithMemberRaw>)
    suspend fun updateOrCreateGroup(group: GroupWithMemberRaw)
    suspend fun updateGroupOnly(group: GroupDocumentRaw)

    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup>
    fun getGroupWithMembers(groupId: String): Flow<UserGroup>
    fun getAllGroups(): Flow<List<UserGroup>>

    suspend fun deleteGroup(groupId: String)
    suspend fun removeMember(groupId: String, userId: String)
    suspend fun clearAll()

}