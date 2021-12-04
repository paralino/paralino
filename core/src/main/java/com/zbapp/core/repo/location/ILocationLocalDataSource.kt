package com.zbapp.core.repo.location

import com.zbapp.core.models.raw.MemberDocumentRaw
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    suspend fun createOrUpdateMembers(vararg data: MemberDocumentRaw)
    suspend fun recreateAllMember(data: List<MemberDocumentRaw>)
    suspend fun recreateMembersForGroup(groupId: String, data: List<MemberDocumentRaw>)

    fun getMemberModelsForUser(userId: String): Flow<MemberDocumentRaw>
    fun getMemberModelsForUsers(userIds: List<String>): Flow<List<MemberDocumentRaw>>
    fun getMembersForGroup(groupId: String): Flow<List<MemberDocumentRaw>>
    fun memberCountInGroup(groupId: String): Flow<Int>

    suspend fun removeMember(id: String)
    suspend fun clearAllMembersForUser(userId: String)
    suspend fun clearAllMembersForGroup(groupId: String)
    suspend fun clearAll()

}