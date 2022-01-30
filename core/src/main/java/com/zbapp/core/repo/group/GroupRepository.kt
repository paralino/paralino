package com.zbapp.core.repo.group

import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return subscribeToData(
            local = local.getAllGroups(),
            remote = {
                remote.getAllGroups()
                    .also { cryptoKEKRepo.refreshAllKeys().first() }
            },
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
            remote = {
                remote.getGroup(id)
                    .also { cryptoKEKRepo.refreshKeysForGroup(id).first() }
            },
            localUpdate = { local.updateOrCreateGroup(it) },
            remotePreference = RemotePreference.PREFER
        )
    }

    fun createGroup(name: String, emoji: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val userData = userRepository.cachedUser!!.data!!
                remote.createGroup(
                    name = name,
                    image = emoji,
                    myData = MemberModelData(userData.userName, userData.imageBase64),
                    myKey = userData.latestAsymmetricKey!!
                ).also { cryptoKEKRepo.refreshKeysForGroup(it.group.id).first() }
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
            remote = { remote.deleteGroup(id) },
            localUpdate = {
                local.deleteGroup(id)
                cryptoKEKRepo.deleteAllKeysForGroup(id)
            },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun addMemberByAccessCode(groupId: String, code: PersonalCodeReadData): Flow<GroupDocumentRaw> {
        return getDataOnce(
            remote = {
                remote.addUserToGroup(
                    groupId,
                    code,
                    userRepository.cachedUser?.data?.latestAsymmetricKey!!
                )
            },
            localUpdate = {
                local.updateOrCreateGroup(it)
                    .also { cryptoKEKRepo.refreshKeysForGroup(groupId).first() }
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
                cryptoKEKRepo.deleteKeysForUser(member.groupId, member.userId)
            },
            remotePreference = RemotePreference.FORCE
        )
    }

    suspend fun clear() {
        local.clearAll()
    }

}