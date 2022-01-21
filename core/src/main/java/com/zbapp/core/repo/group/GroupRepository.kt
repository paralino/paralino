package com.zbapp.core.repo.group

import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val userRepository: UserRepository
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return subscribeToData(
            local = local.getAllGroups(),
            remote = { remote.getAllGroups() },
            localUpdate = { local.recreateGroups(it) },
            remotePreference = RemotePreference.CACHE("groups", 10_000)
        )
    }

    fun getAllGroupsLocally(): Flow<List<UserGroup>> {
        return local.getAllGroups()
    }

    fun getMyGroups(): Flow<List<UserGroup>> {
        return getAllGroups().mapLatest { it.filter { it.isFromUser } }
    }

    fun getGroupWithMembersLocally(id: String): Flow<UserGroup> {
        return local.getGroupWithMembers(id)
    }

    fun getGroupWithoutMembers(id: String): Flow<UserGroup> {
        return local.getGroupWithoutMembers(id)
    }

    fun getGroupWithMembers(id: String): Flow<UserGroup> {
        return subscribeToData(
            local = local.getGroupWithMembers(id),
            remote = { remote.getGroup(id) },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.PREFER
        )
    }

    fun createGroup(name: String, emoji: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val group = UserGroup.new(emoji, name.trim())
                remote.createGroup(group, userRepository.cachedUser!!)
            },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.FORCE
        ).map { Unit }
    }

    fun updateGroup(userGroup: UserGroup): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updateGroup(userGroup) },
            localUpdate = { local.updateGroupOnly(it) },
            remotePreference = RemotePreference.FORCE
        ).map { Unit }
    }

    fun deleteGroup(id: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val group = local.getGroupWithMembers(id).first()
                remote.deleteGroup(group)
            },
            localUpdate = { local.deleteGroup(id) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun addMemberByAccessCode(groupId: String, code: PersonalCodeReadData): Flow<GroupDocumentRaw> {
        return getDataOnce(
            remote = {
                val member = MemberModel(
                    id = "",
                    groupId = groupId,
                    userId = code.ownerId,
                    publicKey = code.ownerKey,
                    data = MemberModelData(code.ownerName, code.ownerImageBase64),
                    mutableData = null
                )
                val group = local.getGroupWithMembers(groupId).first()
                remote.addUserToGroup(group, member, userRepository.cachedUser!!)
            },
            localUpdate = {
                local.updateOrCreateGroup(it)
            },
            remotePreference = RemotePreference.FORCE
        ).map { it.group }
    }

    fun removeMember(member: MemberModel): Flow<Unit> {
        return getDataOnce(
            remote = {
                val group = local.getGroupWithMembers(member.groupId).first()
                remote.removeUserFromGroup(group, member)
            },
            localUpdate = {
                local.removeMember(member.groupId, member.userId)
            },
            remotePreference = RemotePreference.FORCE
        )
    }

    suspend fun clear() {
        local.clearAll()
    }

}