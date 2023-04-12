package app.paralino.core.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.DecryptedGroupKeyData
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull

class CryptoKEKRepository(
    private val remote: CryptoKeyRemoteDataSource,
    private val local: CryptoKeyLocalDataSource,
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

                logg.tag("tag_crypto_key").w("updated group (${it.first.size}) and member (${it.second.size}) keys")
            }
        )
    }

    suspend fun refreshGroupKeysForGroup(groupId: String): List<GroupCryptoKeyDataRaw> {
        return triggerRemoteOnly {
            logg.tag("tag_crypto_key_group").w("refreshing only group key")
            remote.getGroupKeyForGroup(groupId)
        }.also {
            logg.tag("tag_crypto_key_group").w("updated only group keys (${it.size})")

            cachedGroupKeys.value = emptyList()
            local.recreateGroupKeyForGroup(groupId, it)
        }
    }

    suspend fun refreshMemberKeysForGroup(groupId: String): List<GroupMemberCryptoKeyDataRaw> {
        return triggerRemoteOnly {
            logg.tag("tag_crypto_key_group_member").w("refreshing only group member key")
            remote.getAllGroupMemberKeysForGroup(groupId)
        }.also {
            logg.tag("tag_crypto_key_group_member").w("updated only group member (${it.size}) keys")

            cachedMemberKeys.value = emptyList()
            local.recreateMemberKeysForGroup(groupId, it)
        }
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

    suspend fun getLatestGroupKeyVersion(groupId: String): Long? {
        return getGroupKeys().filter { it.groupId == groupId }.maxByOrNull { it.version }?.version
    }

    suspend fun getGroupKey(groupId: String, keyVersion: Long?): DecryptedGroupKeyData? {
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

        val groupKey = try {
            cryptoCore.decryptKeyWithKeyPair(
                key = keyRaw.encryptedGroupKey,
                myPrivateKey = userKeyRepository.latestKeyPair().privateKey,
                publicKey = ownerPublicKey
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group").d("Invalid member key for group $groupId")
            return null
        }

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

    suspend fun getAllEncryptedMemberPublicKeys(groupId: String): List<String> {
       return getMemberKeys().filter { it.groupId == groupId }.map { it.encryptedPublicKey }
    }

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
            ).let { key ->
                // always return own public key from secure storage but ensure no one tampered with key from server
                if(memberUserId == currentUserId) {
                    userKeyRepository.latestKeyPair().publicKey.takeIf { it == key }.also {
                        if(it == null) logg.tag("tag_crypto_key_group_member").w("User $memberUserId own public key does not match in group $groupId")
                    }
                } else {
                    key
                }
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").w("Can not decrypt user $memberUserId public key in group $groupId")
            null
        }

        if(keyRaw.encryptedSenderKey.isNullOrBlank() || memberPublicKey == null) {
            logg.tag("tag_crypto_key_group_member").w("No keys available for user $memberUserId in group $groupId")
            return null
        }

        val memberKey = try {
            cryptoCore.decryptKeyWithKeyPair(
                key = keyRaw.encryptedSenderKey,
                myPrivateKey = userKeyRepository.latestKeyPair().privateKey,
                publicKey = memberPublicKey
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").d("Invalid member key for group $groupId & member $memberUserId")
            return null
        }
        return DecryptedGroupKeyData(keyRaw.version ?: -1, memberKey)
    }

    suspend fun getGroupOwnerPublicKey(groupId: String): String? {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return null
        return getMemberPublicKey(groupId, groupOwnerId)
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
            ).let { key ->
                // always return own public key from secure storage but ensure no one tampered with key from server
                if(memberUserId == currentUserId) {
                    userKeyRepository.latestKeyPair().publicKey.takeIf { it == key }.also {
                        if(it == null) logg.tag("tag_crypto_key_group_member").w("User $memberUserId own public key does not match in group $groupId")
                    }
                } else {
                    key
                }
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").w("Can not decrypt user $memberUserId public key in group $groupId")
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
