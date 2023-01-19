package app.paralino.core.crypto.group

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.DecryptedGroupKeyData
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.*

class CryptoKEKRepository(
    private val scopeProvider: ICoroutineScopeProvider,
    private val remote: CryptoKeyRemoteDataSource,
    private val local: ICryptoKeyLocalDataSource,
    private val userKeyRepository: CryptoUserKeyRepository,
    private val userRepository: UserRepository,
    private val cryptoCore: CryptoCore,
) : BaseRepository() {

    private var cachedGroupKeys = MutableStateFlow<List<GroupCryptoKeyDataRaw>>(emptyList())
    private var cachedMemberKeys = MutableStateFlow<List<GroupMemberCryptoKeyDataRaw>>(emptyList())

    suspend fun getGroupKeys(): List<GroupCryptoKeyDataRaw> {
        return cachedGroupKeys.value.ifEmpty {
            local.getAllGroupKeys()
                .firstOrNull()
                ?.also { cachedGroupKeys.value = it }
                ?: emptyList()
        }
    }

    suspend fun getMemberKeys(): List<GroupMemberCryptoKeyDataRaw> {
        return cachedMemberKeys.value.ifEmpty {
            local.getAllMemberKeys()
                .firstOrNull()
                ?.also { cachedMemberKeys.value = it }
                ?: emptyList()
        }
    }


    fun refreshAllKeys(): Flow<Pair<List<GroupCryptoKeyDataRaw>, List<GroupMemberCryptoKeyDataRaw>>> {
        return getDataOnce(
            remote = {
                logg.tag("tag_crypto_key").w("refreshing group keys")
                val groupKeys = remote.getAllGroupKeys()
                logg.tag("tag_crypto_key").w("refreshing member keys")
                val memberKeys = remote.getAllGroupMemberKeys()
                groupKeys to memberKeys
            },
            localUpdate = {
                cachedGroupKeys.value = it.first
                cachedMemberKeys.value = it.second
                local.recreateAllGroupKeys(it.first, it.second)

                logg.tag("tag_crypto_key").w("updated group and member keys")
            }
        )
    }

    fun refreshAllGroupKeys(): Flow<List<GroupCryptoKeyDataRaw>> {
        return getDataOnce(
            remote = { remote.getAllGroupKeys() },
            localUpdate = {
                cachedGroupKeys.value = it
                local.recreateAllGroupKeys(it)
            }
        )
    }

    fun refreshGroupKeysForGroup(groupId: String): Flow<List<GroupCryptoKeyDataRaw>> {
        return getDataOnce(
            remote = {
                logg.tag("tag_crypto_key_group").w("refreshing only group key")
                remote.getGroupKeyForGroup(groupId)
            },
            localUpdate = {
                logg.tag("tag_crypto_key_group").w("updated only group key")

                cachedGroupKeys.value = emptyList()
                local.recreateGroupKeyForGroup(groupId, it)
            }
        )
    }

    fun refreshMemberKeysForGroup(groupId: String): Flow<List<GroupMemberCryptoKeyDataRaw>> {
        return getDataOnce(
            remote = {
                logg.tag("tag_crypto_key_group_member").w("refreshing only group member key")
                remote.getAllGroupMemberKeysForGroup(groupId)
            },
            localUpdate = {
                logg.tag("tag_crypto_key_group_member").w("updated only group member key")

                cachedMemberKeys.value = emptyList()
                local.recreateMemberKeysForGroup(groupId, it)
            }
        )
    }

    fun refreshKeysForGroup(groupId: String): Flow<Pair<List<GroupCryptoKeyDataRaw>, List<GroupMemberCryptoKeyDataRaw>>> {
        return getDataOnce(
            remote = {
                logg.tag("tag_crypto_key_group").d("refreshing group key")
                val groupKeys = remote.getGroupKeyForGroup(groupId)
                logg.tag("tag_crypto_key_group").d("refreshing group member key")
                val memberKeys = remote.getAllGroupMemberKeysForGroup(groupId)
                groupKeys to memberKeys
            },
            localUpdate = {
                logg.tag("tag_crypto_key_group").d("Got new member keys (${it.second.size}) and group keys (${it.first.size}) for group $groupId")

                cachedGroupKeys.value = emptyList()
                cachedMemberKeys.value = emptyList()
                local.recreateGroupKeyForGroup(groupId, it.first, it.second)

                logg.tag("tag_crypto_key_group").d("updated group and member keys for group")
            }
        )
    }

    fun refreshAllKeysSilently() {
        refreshAllKeys().launchIn(scopeProvider.defaultScope)
    }

    fun refreshKeysForGroupSilently(groupId: String) {
        refreshKeysForGroup(groupId).launchIn(scopeProvider.defaultScope)
    }

    suspend fun getLatestGroupKeyVersion(groupId: String): Long? {
        return getGroupKeys().filter { it.groupId == groupId }.maxByOrNull { it.version }?.version
    }

    suspend fun getGroupKey(groupId: String, keyVersion: Long? = null): DecryptedGroupKeyData? {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return null
        return getGroupKey(groupId, groupOwnerId, keyVersion)
    }

    suspend fun getGroupKey(
        groupId: String,
        groupOwnerId: String,
        keyVersion: Long? = null
    ): DecryptedGroupKeyData? {
        val allGroupKeys = getGroupKeys()
        val keyRaw = allGroupKeys
            .filter { it.groupId == groupId }
            .let { keys ->
                val matchingKey = if (keyVersion != null)
                // if key version is passed get matching key
                    keys.firstOrNull { it.version == keyVersion }
                else
                // otherwise get latest key
                    keys.maxByOrNull { it.version }

                logg.tag("tag_crypto_key_group").d("group keys count: ${allGroupKeys.size} - ${keys.size} for group $groupId, with version ($keyVersion) match: ${matchingKey != null}")

                matchingKey
            }
            ?: return null

        val ownerPublicKey = getMemberPublicKey(groupId, memberUserId = groupOwnerId) ?: return null

        val groupKey = cryptoCore.decryptKeyWithKeyPair(
            key = keyRaw.encryptedGroupKey,
            myPrivateKey = userKeyRepository.latestKeyPair().privateKey,
            publicKey = ownerPublicKey
        )

        return DecryptedGroupKeyData(keyRaw.version, groupKey)
    }

    suspend fun getAllGroupKeys(groupId: String): List<DecryptedGroupKeyData> {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return emptyList()
        val ownerPublicKey = getMemberPublicKey(groupId, memberUserId = groupOwnerId) ?: return emptyList()
        val keys = getGroupKeys().filter { it.groupId == groupId }

        return keys.map {
            val groupKey = cryptoCore.decryptKeyWithKeyPair(
                key = it.encryptedGroupKey,
                myPrivateKey = userKeyRepository.latestKeyPair().privateKey,
                publicKey = ownerPublicKey
            )

            DecryptedGroupKeyData(it.version, groupKey)
        }
    }

    // TODO pass key version
    suspend fun getMemberKey(
        groupId: String,
        memberUserId: String
    ): DecryptedGroupKeyData? {
        val currentUserId = userRepository.cachedUserId
        val allMemberKeys = getMemberKeys()
        val keyRaw = allMemberKeys
            // filter keys that memberUserId created for current user
            .filter { it.groupId == groupId && it.ownerUserId == memberUserId && it.memberUserId == currentUserId }
            .let { keys ->
                logg.tag("tag_crypto_key_group_member").d("member keys count: ${allMemberKeys.size} - ${keys.size} filtered for group $groupId & member $memberUserId")
                keys
            }
            .maxByOrNull { it.version ?: -1 }
            ?: return null

        val memberPublicKey = try {
            cryptoCore.decryptDataWithKey(
                data = keyRaw.encryptedPublicKey,
                key = userKeyRepository.latestKeyPair().publicKey
            )
        } catch (e: Exception) {
            null
        }

        if(keyRaw.encryptedSenderKey == null || memberPublicKey == null)
            return null

        val memberKey = cryptoCore.decryptKeyWithKeyPair(
            key = keyRaw.encryptedSenderKey,
            myPrivateKey = userKeyRepository.latestKeyPair().privateKey,
            publicKey = memberPublicKey
        )
        return DecryptedGroupKeyData(keyRaw.version ?: -1, memberKey)
    }

    suspend fun getMemberPublicKey(
        groupId: String,
        memberUserId: String
    ): String? {
        val currentUserId = userRepository.cachedUserId
        val allMemberKeys = getMemberKeys()
        val keyRaw = allMemberKeys
            // filter keys that memberUserId created for current user
            .filter { it.groupId == groupId && it.ownerUserId == memberUserId && it.memberUserId == currentUserId }
            .let { keys ->
                logg.tag("tag_crypto_key_group_member").d("member keys count: ${allMemberKeys.size} - ${keys.size} filtered for group $groupId & member $memberUserId")
                keys
            }
            .maxByOrNull { it.version ?: -1 }
            ?: return null

        val memberPublicKey = try {
            cryptoCore.decryptDataWithKey(
                data = keyRaw.encryptedPublicKey,
                key = userKeyRepository.latestKeyPair().publicKey
            )
        } catch (e: Exception) {
            null
        }
        return memberPublicKey
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        cachedGroupKeys.value = cachedGroupKeys.value.filter { it.groupId != groupId }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.groupId != groupId }
        local.deleteAllKeysForGroup(groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        cachedMemberKeys.value = cachedMemberKeys.value.filter {
            // remove all keys that were either created by or for userId
            !(it.memberUserId == userId || it.ownerUserId == userId)
        }
        local.deleteKeysForUser(groupId, userId)
    }

    suspend fun clearKeys() {
        cachedMemberKeys.value = emptyList()
        cachedGroupKeys.value = emptyList()
        local.deleteAllKeys()
    }
}
