package com.zbapp.core.repo.group

import com.zbapp.core.crypto.group.CryptoKEKRepository
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.logg
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.group.UserGroupWithMembers
import com.zbapp.core.models.raw.PersonalCodeReadData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.GroupMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.models.user.ImageModel
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
    private val userKeyRepo: CryptoUserKeyRepository,
    private val groupMapperCore: GroupMapperCore
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return mergeDataWithRemote(
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

    fun getAllGroupIds(): Flow<List<String>> {
        return local.getAllGroupIds()
    }

    fun getGroupWithMembersLocally(id: String): Flow<UserGroupWithMembers> {
        return local.getGroupWithMembers(id)
    }

    fun getGroupWithoutMembersLocally(id: String): Flow<UserGroup> {
        return local.getGroupWithoutMembers(id)
    }

    fun getGroupWithMembers(id: String): Flow<UserGroupWithMembers> {
        return mergeDataWithRemote(
            local = local.getGroupWithMembers(id),
            remote = {
                remote.getGroupWithMembers(id)
                    .also { cryptoKEKRepo.refreshKeysForGroup(id).first() }
                    .let { handleGroupKeysUpdate(it) }
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        )
    }

    fun refreshGroupWithMembers(id: String): Flow<GroupWithMemberRaw> {
        return getDataOnce(
            remote = {
                remote.getGroupWithMembers(id)
                // TODO refresh and handle keys
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        )
    }

    fun createGroup(name: String, image: ImageModel): Flow<Unit> {
        return getDataOnce(
            remote = {
                val userData = userRepository.cachedUser!!.data!!
                remote.createGroup(
                    name = name,
                    image = image,
                    myData = MemberModelData(userData.userName, userData.userImage),
                    myKey = userKeyRepo.latestKeyPair()
                ).also { cryptoKEKRepo.refreshKeysForGroup(it.group.id).first() }
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        ).map { Unit }
    }

    fun updateGroup(userGroup: UserGroup): Flow<Unit> {
        return getDataOnce(
            remote = { remote.updateGroup(userGroup) },
            localUpdate = { local.updateGroupOnly(it) }
        ).map { Unit }
    }

    fun deleteGroup(id: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.deleteGroup(id)
            },
            localUpdate = {
                local.deleteGroup(id)
                cryptoKEKRepo.deleteAllKeysForGroup(id)
            }
        )
    }

    fun leaveGroup(id: String): Flow<Unit> {
        return getDataOnce(
            remote = {
                val group = local.getGroupWithoutMembers(id).first()
                remote.removeUserFromGroup(group, userRepository.cachedUserId)
            },
            localUpdate = {
                local.deleteGroup(id)
                cryptoKEKRepo.deleteAllKeysForGroup(id)
            }
        )
    }

    fun addMemberByAccessCode(groupId: String, code: PersonalCodeReadData): Flow<GroupDocumentRaw> {
        return getDataOnce(
            remote = {
                remote.addUserToGroup(
                    groupId,
                    code,
                    userKeyRepo.latestKeyPair()
                )
            },
            localUpdate = {
                local.updateOrCreateGroup(it)
                    .also { cryptoKEKRepo.refreshKeysForGroup(groupId).first() }
            }
        ).map { it.group }
    }

    fun removeMember(member: MemberModel): Flow<Unit> {

        if(member.userId == userRepository.cachedUserId) {
            return leaveGroup(member.groupId)
        }

        return getDataOnce(
            remote = {
                val group = local.getGroupWithoutMembers(member.groupId).first()
                remote.removeUserFromGroup(group, member.userId)
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
        // TODO handle errors
        val remoteGroup = groupMapperCore.remote2CoreWithMembers(group)
        return updateGroupKeyAndRefresh(remoteGroup, force = false)
            ?: updateMemberKeyAndRefresh(remoteGroup)
            ?: group
    }

    private suspend fun updateMemberKeyAndRefresh(remoteGroup: UserGroupWithMembers, force: Boolean = false): GroupWithMemberRaw? {
        val memberMeRemote = remoteGroup.members.firstOrNull { it.userId == userRepository.cachedUserId }

        if(memberMeRemote?.shouldRotateKeys == true || force) {
            logg.tag("tag_group_key_member").w("group member key is deprecated - rotating: ${remoteGroup.id} - ${memberMeRemote?.userId}")
            cryptoKEKRepo.rotateAllMemberKeys(remoteGroup)
            return getDataOnce(remote = { remote.getGroupWithMembers(remoteGroup.id) }).first()
        } else {
            logg.tag("tag_group_key_member").w("not rotating member keys: ${remoteGroup.id} - ${memberMeRemote?.userId}")
        }

        return null
    }

    private suspend fun updateGroupKeyAndRefresh(remoteGroup: UserGroupWithMembers, force: Boolean = false): GroupWithMemberRaw? {
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