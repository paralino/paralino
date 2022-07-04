package app.paralino.core.repo.group

import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMemberRaw
import kotlinx.coroutines.flow.Flow

interface IGroupLocalDataSource {

    suspend fun recreateGroups(data: List<GroupWithMemberRaw>)
    suspend fun updateOrCreateGroup(group: GroupWithMemberRaw)
    suspend fun updateGroupOnly(group: GroupDocumentRaw)

    fun getGroupWithoutMembers(groupId: String): Flow<UserGroup>
    fun getGroupWithMembers(groupId: String): Flow<UserGroupWithMembers>
    fun getAllGroups(): Flow<List<UserGroup>>
    fun getAllGroupIds(): Flow<List<String>>

    suspend fun deleteGroup(groupId: String)
    suspend fun removeMember(groupId: String, userId: String)
    suspend fun clearAll()

}