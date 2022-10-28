package app.paralino.core.repo.group

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.user.UserPersonalCodeData
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.location.MemberModelData
import app.paralino.core.models.mappers.GroupMapperCore
import app.paralino.core.models.raw.GroupDocumentRaw
import app.paralino.core.models.raw.GroupWithMemberRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.NotFoundException
import app.paralino.core.utils.exceptions.asRemoteException
import kotlinx.coroutines.flow.*

class GroupRepository constructor(
    private val local: IGroupLocalDataSource,
    private val remote: GroupsRemoteSource,
    private val userRepository: UserRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val groupMapperCore: GroupMapperCore,
    private val placesRepo: PlacesRepository
) : BaseRepository() {

    fun getAllGroups(): Flow<List<UserGroup>> {
        return mergeDataWithRemote(
            local = local.getAllGroups(),
            remote = {
                logg.tag("tag_crypto_key").w("refreshing all groups and places")
                refreshAllGroupsWithPlaces()
            },
            localUpdate = {},
            remotePreference = RemotePreference.CACHE("groups", 3_000)
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
                getRemoteGroupWithMembers(id)
                    .also { cryptoKEKRepo.refreshKeysForGroup(id).first() }
                    .let { handleGroupKeysUpdate(it) }
            },
            localUpdate = { local.updateOrCreateGroup(it) }
        )
    }

    suspend fun refreshAllGroupsWithPlaces() {
        refreshAllGroups()
        placesRepo.refreshAllPlaces()
    }

    suspend fun refreshAllGroups() {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key").w("refreshing all groups")
            remote.getAllGroups()
                .also { cryptoKEKRepo.refreshAllKeys().first() }
                .let { it.map { handleGroupKeysUpdate(it) } }
        }
            .also {
                logg.tag("tag_crypto_key_group").d("group and keys ready, updating groups")
                local.recreateGroups(it)
            }
    }

    suspend fun refreshGroupWithMembers(id: String): GroupWithMemberRaw {
        // TODO refresh and handle keys
        return getRemoteGroupWithMembers(id).also { local.updateOrCreateGroup(it) }
    }

    private suspend fun getRemoteGroupWithMembers(groupId: String): GroupWithMemberRaw {
        return try {
            triggerRemoteOnly { remote.getGroupWithMembers(groupId) }
        } catch (e: Exception) {
            if (e.asRemoteException() is NotFoundException) {
                clearGroupLocally(groupId)
            }
            throw e
        }
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
                clearGroupLocally(id)
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
                clearGroupLocally(id)
            }
        )
    }

    fun addMemberByAccessCode(groupId: String, code: UserPersonalCodeData): Flow<GroupDocumentRaw> {
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
        return removeMember(groupId = member.groupId, memberUserId = member.userId)
    }

    fun removeMember(groupId: String, memberUserId: String): Flow<Unit> {

        if (memberUserId == userRepository.cachedUserId) {
            return leaveGroup(groupId)
        }

        return getDataOnce(
            remote = {
                val group = local.getGroupWithoutMembers(groupId).first()
                remote.removeUserFromGroup(group, memberUserId)
            },
            localUpdate = {
                local.removeMember(groupId, memberUserId)
                cryptoKEKRepo.deleteKeysForUser(groupId, memberUserId)
            }
        )
            .onEach {
                val group = local.getGroupWithMembers(groupId).first()
                updateGroupAndMemberKeysAndRefresh(group, force = true)?.let {
                    local.updateOrCreateGroup(it)
                }
            }
    }

    private suspend fun handleGroupKeysUpdate(group: GroupWithMemberRaw): GroupWithMemberRaw {
        // TODO handle errors
        logg.tag("tag_crypto_key_group").w("group and member keys updated")
        val remoteGroup = groupMapperCore.remote2CoreWithMembers(group)
        return updateGroupAndMemberKeysAndRefresh(remoteGroup, force = false)
            ?: updateMemberKeyAndRefresh(remoteGroup)
            ?: group
    }

    private suspend fun updateMemberKeyAndRefresh(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false,
    ): GroupWithMemberRaw? {

        val shouldRotateKeys = remoteGroup.members.any {
            // check if any of current user member documents have key rotate flag set
            it.userId == userRepository.cachedUserId && it.shouldRotateKeys
        }

        if (shouldRotateKeys || force) {
            logg.tag("tag_crypto_key_group_member")
                .w("group member key is deprecated - rotating: ${remoteGroup.id}")
            cryptoKEKRepo.rotateAllMemberKeys(remoteGroup)
            return getDataOnce(remote = { getRemoteGroupWithMembers(remoteGroup.id) }).first()
        } else {
            logg.tag("tag_crypto_key_group_member")
                .w("not rotating member keys, create missing instead: ${remoteGroup.id}")
            cryptoKEKRepo.createMissingMemberKeys(remoteGroup)
        }

        return null
    }

    private suspend fun updateGroupAndMemberKeysAndRefresh(
        remoteGroup: UserGroupWithMembers,
        force: Boolean = false,
    ): GroupWithMemberRaw? {
        if (remoteGroup.ownerUserId != userRepository.cachedUserId) return null
        if (!force && !remoteGroup.shouldRotateKeys) return null

        logg.tag("tag_crypto_key_group").w("group key is deprecated - rotating: ${remoteGroup.id}")

        val places = placesRepo.getPlacesForGroupLocally(remoteGroup.id).first()

        // this will rotate and refresh both group and member keys
        cryptoKEKRepo.rotateAllGroupKeys(remoteGroup, places)
        return getDataOnce(remote = { getRemoteGroupWithMembers(remoteGroup.id) }).first()
    }

    private suspend fun clearGroupLocally(groupId: String) {
        local.deleteGroup(groupId)
        placesRepo.deleteLocallyForGroup(groupId)
        cryptoKEKRepo.deleteAllKeysForGroup(groupId)
    }

    suspend fun clear() {
        local.clearAll()
        placesRepo.clear()
    }

}