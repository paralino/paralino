package app.paralino.core.repo.location

import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    suspend fun createOrUpdateMembers(vararg data: MemberDocumentRaw)
    suspend fun recreateAllMember(data: List<MemberWithLocationsRaw>)
    suspend fun recreateMembersForGroup(groupId: String, data: List<MemberWithLocationsRaw>)

    fun getMemberRawPresentation(memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>>
    fun getMemberModelsForUser(userId: String): Flow<List<MemberModelWithLocations>>
    fun getMembersForGroup(groupId: String): Flow<List<MemberModelWithLocations>>
    fun getMemberCountInGroup(groupId: String): Flow<Int>
    suspend fun getMemberCountInGroupOnce(groupId: String): Int

    suspend fun getUserLocationsIds(groupId: String, userId: String): List<String>
    suspend fun getUserMemberId(groupId: String, userId: String): String?

    suspend fun clearMemberLocationData(locationId: String)
    suspend fun removeAllMembersForGroup(groupId: String)
    suspend fun removeMemberFromGroup(groupId: String, memberId: String)
    suspend fun clearAll()

}