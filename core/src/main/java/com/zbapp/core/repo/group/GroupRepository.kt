package com.zbapp.core.repo.group

import com.zbapp.core.crypto.CryptoKEKRepository
import com.zbapp.core.crypto.DecryptedGroupKeyData
import com.zbapp.core.logg
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.invite.PersonalCodeReadData
import com.zbapp.core.models.location.MemberModel
import com.zbapp.core.models.location.MemberModelData
import com.zbapp.core.models.mappers.GroupDataMapperCore
import com.zbapp.core.models.mappers.MemberMapperCore
import com.zbapp.core.models.raw.GroupDocumentRaw
import com.zbapp.core.models.raw.GroupWithMemberRaw
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val groupMapperCore: GroupDataMapperCore,
    private val groupMemberMapperCore: MemberMapperCore
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return subscribeToData(
            local = local.getAllGroups(),
            remote = {
                remote.getAllGroups()
                    .also { cryptoKEKRepo.refreshAllKeys().first() }
            },
            localUpdate = { groups ->
                groups.map { group ->
                    val remoteGroup = groupMapperCore.remote2Core(group.group)
                        .copy(members = group.members.map {
                            groupMemberMapperCore.remote2Core(
                                it,
                                group.group.ownerPublicKey
                            )
                        })
                    updateGroupKeyIfNeeded(remoteGroup)
                        ?: updateGroupMemberKeyIfNeeded(remoteGroup)
                        ?: group
                }.let {
                    local.recreateGroups(it)
                }
            },
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
            },
            localUpdate = { group ->
                val remoteGroup = groupMapperCore.remote2Core(group.group)
                    .copy(members = group.members.map {
                        groupMemberMapperCore.remote2Core(
                            it,
                            group.group.ownerPublicKey
                        )
                    })
                val updatedGroup = updateGroupKeyIfNeeded(remoteGroup)
                    ?: updateGroupMemberKeyIfNeeded(remoteGroup)
                    ?: group
                local.updateOrCreateGroup(updatedGroup)
            },
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
            },
            remotePreference = RemotePreference.FORCE
        )
            .onEach {
                val group = local.getGroupWithMembers(member.groupId).first()
                rotateGroupKeyAndRefresh(group).let {
                    local.updateOrCreateGroup(it)
                }
            }
    }

    private suspend fun updateGroupMemberKeyIfNeeded(remoteGroup: UserGroup): GroupWithMemberRaw? {
        val memberMeRemote = remoteGroup.members.firstOrNull { it.userId == userRepository.cachedUserId }

        if(memberMeRemote?.shouldRotateKeys == true) {
            logg.tag("tag_group_key_member").w("group member key is deprecated - rotating: ${remoteGroup.id} - ${memberMeRemote.userId}")
            return rotateGroupMemberKeyAndRefresh(remoteGroup)
        }

        return null
    }

    private suspend fun updateGroupKeyIfNeeded(remoteGroup: UserGroup): GroupWithMemberRaw? {
        if (remoteGroup.ownerUserId != userRepository.cachedUserId) return null
        if (!remoteGroup.shouldRotateKeys) return null

        logg.tag("tag_group_key").w("group key is deprecated - rotating: ${remoteGroup.id}")
        return rotateGroupKeyAndRefresh(remoteGroup)
    }

    private suspend fun rotateGroupKeyAndRefresh(remoteGroup: UserGroup): GroupWithMemberRaw {
        val userKey = userRepository.cachedUser!!.data!!.latestAsymmetricKey!!

        // 1. rotate group key
        cryptoKEKRepo.rotateGroupKey(group = remoteGroup, myKey = userKey).first()

        // 2. fetch new key(s) for that group
        cryptoKEKRepo.refreshGroupKeysForGroup(groupId = remoteGroup.id).first()

        // 3. rotate member keys
        // new group key is required first since we are re-encrypting user data with it
        cryptoKEKRepo.rotateGroupMemberKey(
            group = remoteGroup,
            myKey = userKey
        ).first()

        // 4. refresh member keys
        cryptoKEKRepo.refreshMemberKeysForGroup(groupId = remoteGroup.id).first()

        // 5. finally refresh group again to get new data and states
        return getDataOnce(
            remote = { remote.getGroupWithMembers(remoteGroup.id) },
            remotePreference = RemotePreference.FORCE
        ).first()
    }

    private suspend fun rotateGroupMemberKeyAndRefresh(group: UserGroup): GroupWithMemberRaw {
        val userKey = userRepository.cachedUser!!.data!!.latestAsymmetricKey!!
        cryptoKEKRepo.rotateGroupMemberKey(group = group, myKey = userKey).first()
        cryptoKEKRepo.refreshKeysForGroup(groupId = group.id).first()
        return getDataOnce(
            remote = { remote.getGroupWithMembers(group.id) },
            remotePreference = RemotePreference.FORCE
        ).first()
    }

    suspend fun clear() {
        local.clearAll()
    }

}