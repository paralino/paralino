package com.zbapp.core.crypto

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.logg
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*
import javax.swing.GroupLayout


class CryptoKEKRepository(
    private val scopeProvider: ICoroutineScopeProvider,
    private val remote: CryptoKeyRemoteDataSource,
    private val local: ICryptoKeyLocalDataSource,
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore
) : BaseRepository() {

    private val groupKeys = local.getAllGroupKeys()
        .stateIn(
            scope = scopeProvider.defaultScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val memberKeys = local.getAllMemberKeys()
        .stateIn(
            scope = scopeProvider.defaultScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun refreshAllKeys(): Flow<Pair<List<GroupCryptoKeyDataRaw>, List<GroupMemberCryptoKeyDataRaw>>> {
        return getDataOnce(
            remote = {
                val groupKeys = remote.getAllGroupKeys()
                val memberKeys = remote.getAllGroupMemberKeys()
                groupKeys to memberKeys
            },
            localUpdate = {
                local.recreateAllGroupKeys(it.first)
                local.recreateAllMemberKeys(it.second)
            },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun refreshAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return getDataOnce(
            remote = { remote.getAllGroupKeys() },
            localUpdate = { local.recreateAllGroupKeys(it) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun refreshGroupKeysForGroup(groupId: String): Flow<List<GroupCryptoKeyDataRaw>> {
        return getDataOnce(
            remote = { remote.getGroupKeyForGroup(groupId) },
            localUpdate = { local.recreateGroupKeyForGroup(groupId, it) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun refreshMemberKeysForGroup(groupId: String): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return getDataOnce(
            remote = { remote.getAllGroupMemberKeysForGroup(groupId) },
            localUpdate = { local.recreateMemberKeysForGroup(groupId, it) },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun refreshKeysForGroup(groupId: String): Flow<Pair<List<GroupCryptoKeyDataRaw>, List<GroupMemberCryptoKeyDataRaw>>> {
        return getDataOnce(
            remote = {
                val groupKeys = remote.getGroupKeyForGroup(groupId)
                val memberKeys = remote.getAllGroupMemberKeysForGroup(groupId)
                groupKeys to memberKeys
            },
            localUpdate = {
                local.recreateGroupKeyForGroup(groupId, it.first)
                local.recreateMemberKeysForGroup(groupId, it.second)
            },
            remotePreference = RemotePreference.FORCE
        )
    }

    fun refreshAllKeysSilently() {
        refreshAllKeys().launchIn(scopeProvider.defaultScope)
    }

    fun refreshKeysForGroupSilently(groupId: String) {
        refreshKeysForGroup(groupId).launchIn(scopeProvider.defaultScope)
    }

    suspend fun hasGroupKey(groupId: String, vararg keyVersions: Long): Boolean {
        val keys = groupKeys.value.filter { it.groupId == groupId }
        return keyVersions.all { version ->
            keys.any { it.version == version }
        }
    }

    fun hasMemberKey(
        groupId: String,
        memberUserId: String,
        keyVersion: Long
    ): Boolean {
        return memberKeys.value
            .filter { it.groupId == groupId && it.memberUserId == memberUserId}
            .any { it.version == keyVersion }
    }

    fun hasMemberKeys(
        groupId: String,
        keyVersion: List<Pair<String, Long>>
    ): Boolean {
        val keys = memberKeys.value.filter { it.groupId == groupId }
        return keyVersion
            .all { key ->
                keys.any { it.memberUserId == key.first && it.version == key.second }
            }
    }

    fun getLatestGroupKeyVersion(groupId: String): Long? {
        return groupKeys.value.filter { it.groupId == groupId }.maxByOrNull { it.version }?.version
    }

    suspend fun getLatestGroupKey(groupId: String): DecryptedGroupKeyData? {
        val ownerPublicKey = local.getGroupOwnerPublicKey(groupId) ?: return null
        return getGroupKey(groupId, ownerPublicKey)
    }

    suspend fun getGroupKey(groupId: String, ownerPublicKey: String, keyVersion: Long? = null): DecryptedGroupKeyData? {
        val keyRaw = groupKeys.value
            .filter { it.groupId == groupId }
            .let {
                if(keyVersion != null)
                    // if key version is passed get matching key
                    it.firstOrNull { it.version == keyVersion }
                else
                    // otherwise get latest key
                    it.maxByOrNull { it.version }
            }
            ?: return null

        val groupKey = cryptoCore.decryptKeyWithKeyPair(
            key = keyRaw.encryptedGroupKey,
            myPrivateKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!.privateKey,
            publicKey = ownerPublicKey
        )

        return DecryptedGroupKeyData(keyRaw.version, groupKey)
    }

    suspend fun getAllGroupKeys(groupId: String): List<DecryptedGroupKeyData> {
        val ownerPublicKey = local.getGroupOwnerPublicKey(groupId) ?: return emptyList()
        val keys = groupKeys.value.filter { it.groupId == groupId }

        return keys.map {
            val groupKey = cryptoCore.decryptKeyWithKeyPair(
                key = it.encryptedGroupKey,
                myPrivateKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!.privateKey,
                publicKey = ownerPublicKey
            )

            DecryptedGroupKeyData(it.version, groupKey)
        }

    }

    suspend fun getMemberKey(groupId: String, memberUserId: String): DecryptedGroupKeyData? {
        val memberPublicKey = local.getMemberPublicKey(groupId, memberUserId) ?: return null
        return getMemberKey(groupId, memberUserId, memberPublicKey)
    }

    suspend fun getMemberKey(groupId: String, memberUserId: String, memberPublicKey: String): DecryptedGroupKeyData? {
        val keyRaw = memberKeys.value
            .filter { it.groupId == groupId && it.ownerUserId == memberUserId }
            .maxByOrNull { it.version }
            ?: return null

        val memberKey = cryptoCore.decryptKeyWithKeyPair(
            key = keyRaw.encryptedSenderKey,
            myPrivateKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!.privateKey,
            publicKey = memberPublicKey
        )
        return DecryptedGroupKeyData(keyRaw.version, memberKey)
    }

    suspend fun rotateAllGroupKeys(remoteGroup: UserGroup) {
        val userKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!

        // 1. rotate group key
        getDataOnce(
            remote = { remote.rotateGroupKeys(group = remoteGroup, myKey = userKey) }
        ).first()

        // 2. fetch new key(s) for that group
        refreshGroupKeysForGroup(groupId = remoteGroup.id).first()

        // 3. rotate member keys
        // new group key is required first since we are re-encrypting user data with it
        rotateGroupMemberKey(group = remoteGroup, myKey = userKey).first()

        // 4. refresh member keys
        refreshMemberKeysForGroup(groupId = remoteGroup.id).first()
    }

    suspend fun rotateAllMemberKeys(remoteGroup: UserGroup) {
        val userKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!
        rotateGroupMemberKey(group = remoteGroup, myKey = userKey).first()
        refreshKeysForGroup(groupId = remoteGroup.id).first()
    }

    private suspend fun rotateGroupMemberKey(
        group: UserGroup,
        myKey: AsymmetricCryptoKeyData
    ): Flow<Unit> {
        return getDataOnce(
            remote = {
                remote.rotateMemberKeysForGroup(
                    group = group,
                    groupKey = getLatestGroupKey(group.id)!!,
                    myKey = myKey
                )
            }
        ).map { Unit }
    }

    suspend fun deleteGroupKeyForGroup(groupId: String) {
        local.deleteGroupKeyForGroup(groupId)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        local.deleteAllKeysForGroup(groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        local.deleteKeysForUser(groupId, userId)
    }

    suspend fun clearKeys() {
        local.deleteAllKeys()
    }
}

data class DecryptedGroupKeyData(
    val version: Long,
    val data: Data
)