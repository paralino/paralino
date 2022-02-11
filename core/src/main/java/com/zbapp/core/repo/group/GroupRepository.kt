package com.zbapp.core.repo.group

import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.logg
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.GroupMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupMapperCore: GroupMapperCore
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return subscribeToData(
            local = local.getAllGroups(),
            remote = {
                remote.getAllGroups()
                    .also { cryptoKEKRepo.refreshAllKeys().first() }
                    .let { it.map { handleGroupKeysUpdate(it) } }
            },
            localUpdate = { local.recreateGroups(it) },
            remotePreference = RemotePreference.CACHE("groups", 10_000)
        )
    }

    fun getAllGroupsLocally(): Flow<List<UserGroup>> {
        return local.getAllGroups()
    }

    fun getGroupWithMembersLocally(id: String): Flow<UserGroup> {
        return local.getGroupWithMembers(id)
    }

    fun getGroupWithoutMembersLocally(id: String): Flow<UserGroup> {
        return local.getGroupWithoutMembers(id)
    }

    fun getGroupWithMembers(id: String): Flow<UserGroup> {
        return subscribeToData(
            local = local.getGroupWithMembers(id),
            remote = {
                remote.getGroupWithMembers(id)
                    .also { cryptoKEKRepo.refreshKeysForGroup(id).first() }
                    .let { handleGroupKeysUpdate(it) }
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
        // TODO differentiate between removing user and leaving group
        return getDataOnce(
            remote = {
                val group = local.getGroupWithMembers(member.groupId).first()
                remote.removeUserFromGroup(group, member)
            },
            localUpdate = {
                local.removeMember(member.groupId, member.userId)
                cryptoKEKRepo.deleteKeysForUser(member.groupId, member.userId)
            }
        )
            .onEach {
                val group = local.getGroupWithMembers(member.groupId).first()
                updateGroupKeyAndRefresh(group, force = true)?.let {
                    local.updateOrCreateGroup(it)
                }
            }
    }

    private suspend fun handleGroupKeysUpdate(group: GroupWithMemberRaw): GroupWithMemberRaw {
        val remoteGroup = groupMapperCore.remote2CoreWithMembers(group)
        return updateGroupKeyAndRefresh(remoteGroup, force = false)
            ?: updateMemberKeyAndRefresh(remoteGroup)
            ?: group
    }

    private suspend fun updateMemberKeyAndRefresh(remoteGroup: UserGroup, force: Boolean = false): GroupWithMemberRaw? {
        val memberMeRemote = remoteGroup.members.firstOrNull { it.userId == userRepository.cachedUserId }

        if(memberMeRemote?.shouldRotateKeys == true || force) {
            logg.tag("tag_group_key_member").w("group member key is deprecated - rotating: ${remoteGroup.id} - ${memberMeRemote?.userId}")
            cryptoKEKRepo.rotateAllMemberKeys(remoteGroup)
            return getDataOnce(remote = { remote.getGroupWithMembers(remoteGroup.id) }).first()
        }

        return null
    }

    private suspend fun updateGroupKeyAndRefresh(remoteGroup: UserGroup, force: Boolean = false): GroupWithMemberRaw? {
        if (remoteGroup.ownerUserId != userRepository.cachedUserId) return null
        if (!force && !remoteGroup.shouldRotateKeys) return null

        logg.tag("tag_group_key").w("group key is deprecated - rotating: ${remoteGroup.id}")
        cryptoKEKRepo.rotateAllGroupKeys(remoteGroup)
        return getDataOnce(remote = { remote.getGroupWithMembers(remoteGroup.id) }).first()
    }

    suspend fun clear() {
        local.clearAll()
    }

}