package app.paralino.core.repo.location

import app.paralino.core.models.location.MemberModelWithLocations
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.MemberWithLocationsRaw
import kotlinx.coroutines.flow.Flow

interface ILocationLocalDataSource {

    suspend fun createOrUpdateMembersWithoutSettings(vararg data: MemberDocumentRaw)
    suspend fun createOrUpdateMembersWithSettings(vararg data: MemberDocumentRaw)
    suspend fun createOrUpdateMemberLocation(data: MemberLocationRaw)
    suspend fun recreateMembers(vararg data: MemberWithLocationsRaw)
    suspend fun recreateAllMembers(data: List<MemberWithLocationsRaw>)
    suspend fun recreateMembersForGroup(groupId: String, data: List<MemberWithLocationsRaw>)

    fun getMemberRawPresentation(memberId: String): Flow<Pair<MemberWithLocationsRaw, MemberModelWithLocations>>
    fun getMemberModelsForUser(userId: String): Flow<List<MemberModelWithLocations>>
    fun getMembersForGroup(groupId: String): Flow<List<MemberModelWithLocations>>
    fun getMember(groupId: String, userId: String): Flow<MemberModelWithLocations>
    suspend fun getMemberOnce(groupId: String, userId: String): MemberModelWithLocations?
    fun getMemberCountInGroup(groupId: String): Flow<Int>
    suspend fun getMemberCountInGroupOnce(groupId: String): Int

    suspend fun getUserLocationsIds(groupId: String, userId: String): List<String>
    suspend fun getUserMemberId(groupId: String, userId: String): String?

    suspend fun clearMemberLocationData(locationId: String)
    suspend fun removeAllMembersForGroup(groupId: String)
    suspend fun removeMemberFromGroup(groupId: String, memberId: String)
    suspend fun clearAll()

}