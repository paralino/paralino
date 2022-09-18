package app.paralino.core.repo.location

import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.raw.MemberDocumentRaw
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    suspend fun createOrUpdateMembers(vararg data: MemberDocumentRaw)
    suspend fun recreateAllMember(data: List<MemberDocumentRaw>)
    suspend fun recreateMembersForGroup(groupId: String, data: List<MemberDocumentRaw>)

    fun getMemberModelsForUser(userId: String): Flow<MemberModel>
    fun getMemberModelsForUsers(userIds: List<String>): Flow<List<MemberModel>>
    fun getMembersForGroup(groupId: String): Flow<List<MemberModel>>
    suspend fun memberCountInGroup(groupId: String): Int
    suspend fun getUserMemberIds(userId: String, groupId: String): List<String>

    suspend fun removeMember(id: String)
    suspend fun clearMemberLocationData(memberId: String)
    suspend fun clearAllMembersForUser(userId: String)
    suspend fun clearAllMembersForGroup(groupId: String)
    suspend fun clearAllMembersForGroup(groupId: String, userId: String)
    suspend fun clearAll()

}