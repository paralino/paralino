package com.zbapp.core.crypto

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.models.raw.GroupCryptoKeyDataRaw
import com.zbapp.core.models.raw.GroupMemberCryptoKeyDataRaw
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.base.RemotePreference
import com.zbapp.core.repo.group.IGroupLocalDataSource
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class CryptoKeyRepository(
    private val userRepo: UserRepository,
    private val cryptoAuth: CryptoAuth,
    private val groupsLocal: IGroupLocalDataSource,
    private val local: ICryptoKeyLocalDataSource,
    private val remote: CryptoKeyRemoteDataSource,
    private val scopeProvider: ICoroutineScopeProvider
) : BaseRepository() {

    fun refreshAllKeys() {
        getDataOnce(
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

    fun refreshKeysForGroup(groupId: String) {
        getDataOnce(
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

    val groupKeys = local.getAllGroupKeys()
        .stateIn(
            scope = scopeProvider.defaultScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val memberKeys = local.getAllMemberKeys()
        .stateIn(
            scope = scopeProvider.defaultScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    suspend fun getGroupKey(groupId: String): String? {
        val ownerPublicKey = groupsLocal.getGroupOwnerPublicKey(groupId) ?: return null
        return getGroupKey(groupId, ownerPublicKey)
    }

    suspend fun getGroupKey(groupId: String, ownerPublicKey: String): String? {
        val keyRaw = groupKeys.value.firstOrNull { it.groupId == groupId }
            ?: return null

        val groupKey = cryptoAuth.decryptDataWithKeyPair(
            data = keyRaw.encryptedGroupKey,
            myPrivateKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!.privateKey,
            publicKey = ownerPublicKey
        )

        return groupKey
    }

    suspend fun getMemberKey(groupId: String, memberId: String, memberPublicKey: String): String? {
        val keyRaw = memberKeys.value.firstOrNull { it.groupId == groupId && it.memberId == memberId }
            ?: return null

        val memberKey = cryptoAuth.decryptDataWithKeyPair(
            data = keyRaw.encryptedSenderKey,
            myPrivateKey = userRepo.cachedUser!!.data!!.latestAsymmetricKey!!.privateKey,
            publicKey = memberPublicKey
        )
        return memberKey
    }

    fun listenToGroupKey(groupId: String): Flow<GroupCryptoKeyDataRaw?> {
        return groupKeys.map { it.firstOrNull { it.groupId == groupId } }
    }

    fun listenToMemberKey(groupId: String, memberId: String): Flow<GroupMemberCryptoKeyDataRaw?> {
        return memberKeys.map { it.firstOrNull { it.groupId == groupId && it.memberId == memberId } }
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

    suspend fun reset() {
        local.deleteAllKeys()
        cryptoAuth.logout()
    }

    suspend fun encryptData(data: String): String {
        return cryptoAuth.encryptData(data)
    }

    suspend fun decryptData(data: String): String {
        return cryptoAuth.decryptData(data)
    }
}