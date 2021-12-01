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

    suspend fun clearForMember(id: String)
    suspend fun clearForUser(userId: String)
    suspend fun clearForGroup(groupId: String)
    suspend fun clearAll()

}