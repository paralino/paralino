package app.paralino.core.repo.group

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMembersRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun recreateGroups(data: List<GroupWithMembersRaw>)
    suspend fun updateOrCreateGroup(group: GroupWithMembersRaw)
    suspend fun updateGroupOnly(group: GroupDocumentRaw)

    fun getGroupRawPresentation(groupId: String): Flow<Pair<GroupDocumentRaw, UserGroup>>
    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup>
    fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers>
    fun getAllGroups(): Flow<List<UserGroup>>
    fun getAllGroupIds(): Flow<List<String>>

    suspend fun deleteGroup(groupId: String)
    suspend fun removeMember(groupId: String, userId: String)
    suspend fun clearAll()

}