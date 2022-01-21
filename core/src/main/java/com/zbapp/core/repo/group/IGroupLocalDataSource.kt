package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun recreateGroups(data: List<GroupWithMemberRaw>)
    suspend fun updateOrCreateGroup(group: GroupWithMemberRaw)
    suspend fun updateGroupOnly(group: GroupDocumentRaw)

    suspend fun recreateGroupKeys(keys: List<GroupCryptoKeyDataRaw>)
    suspend fun recreateGroupMemberKeys(keys: List<GroupMemberCryptoKeyDataRaw>)
    suspend fun saveGroupKeys(vararg keys: GroupCryptoKeyDataRaw)
    suspend fun saveGroupMemberKeys(vararg keys: GroupMemberCryptoKeyDataRaw)

    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup>
    fun getGroupWithMembers(groupId: String): Flow<UserGroup>
    fun getAllGroups(): Flow<List<UserGroup>>

    suspend fun deleteGroup(groupId: String)
    suspend fun removeMember(groupId: String, userId: String)
    suspend fun clearAll()

}