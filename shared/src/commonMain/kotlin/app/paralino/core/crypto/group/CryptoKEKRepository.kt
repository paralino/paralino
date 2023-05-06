package app.paralino.core.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalPreKeyRaw
import app.paralino.core.models.user.SimpleKeyPairData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.decodeFromString

class CryptoKEKRepository(
    private val remote: CryptoKeyRemoteDataSource,
    private val local: CryptoKeyLocalDataSource,
    private val userKeyRepository: CryptoUserKeyRepository,
    private val userRepository: UserRepository,
    private val cryptoCore: CryptoCore,
) : BaseRepository() {

    private var cachedGroupKeys = MutableStateFlow<List<GroupCryptoKeyDataRaw>>(emptyList())
    private var cachedMemberKeys = MutableStateFlow<List<GroupMemberCryptoKeyDataRaw>>(emptyList())
    private var cachedPreKeys = MutableStateFlow<List<PersonalPreKeyRaw>>(emptyList())

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

    suspend fun getPreKeys(): List<PersonalPreKeyRaw> {
        return cachedPreKeys.value.ifEmpty {
            local.getAllPreKeys()
                .firstOrNull()
                ?.also { cachedPreKeys.value = it }
                ?: emptyList()
        }
    }


    suspend fun refreshAllKeys() {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key").w("refreshing group keys")
            val groupKeys = remote.getAllGroupKeys()
            logg.tag("tag_crypto_key").w("refreshing member keys")
            val memberKeys = remote.getAllGroupMemberKeys()
            logg.tag("tag_crypto_key").w("refreshing pre keys")
            val preKeys = remote.getAllGroupPreKeys()

            cachedGroupKeys.value = groupKeys
            cachedMemberKeys.value = memberKeys
            cachedPreKeys.value = preKeys

            local.recreateAllGroupKeys(groupKeys, memberKeys, preKeys)

            logg.tag("tag_crypto_key").w("updated group (${groupKeys.size}), member (${memberKeys.size}), pre (${preKeys.size}) keys")
        }
    }

    suspend fun refreshGroupKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group").w("refreshing only group key")
            remote.getGroupKeyForGroup(groupId)
        }.also {
            logg.tag("tag_crypto_key_group").w("updated only group keys (${it.size})")

            cachedGroupKeys.value = emptyList()
            local.recreateGroupKeyForGroup(groupId, it)
        }
    }

    suspend fun refreshMemberKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group_member").w("refreshing only group member key")
            val memberKeys = remote.getAllGroupMemberKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member").w("refreshing group pre keys")
            val preKeys = remote.getAllGroupPreKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member").w("updated member (${memberKeys.size}), pre (${preKeys.size}) keys")

            cachedMemberKeys.value = emptyList()
            cachedPreKeys.value = emptyList()
            local.recreateMemberKeysForGroup(groupId, memberKeys, preKeys)
        }
    }

    suspend fun refreshKeysForGroup(groupId: String){
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group").d("refreshing group key")
            val groupKeys = remote.getGroupKeyForGroup(groupId)
            logg.tag("tag_crypto_key_group").d("refreshing group member key")
            val memberKeys = remote.getAllGroupMemberKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group").w("refreshing group pre keys")
            val preKeys = remote.getAllGroupPreKeysForGroup(groupId)

            logg.tag("tag_crypto_key_group").d("Got new group (${groupKeys.size}), member (${memberKeys.size}), pre (${preKeys.size}) for group $groupId")

            cachedGroupKeys.value = emptyList()
            cachedMemberKeys.value = emptyList()
            cachedPreKeys.value = emptyList()
            local.recreateGroupKeyForGroup(groupId, groupKeys, memberKeys, preKeys)

            logg.tag("tag_crypto_key_group").d("updated group and member keys for group")
        }
    }

    suspend fun getLatestGroupKeyVersion(groupId: String): Long? {
        return getGroupKeys().filter { it.groupId == groupId }.maxByOrNull { it.version }?.version
    }

    suspend fun getGroupKey(groupId: String, keyVersion: Long?): GroupMasterKey {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return GroupMasterKey.Error.Missing
        return getGroupKey(groupId, groupOwnerId, keyVersion)
    }

    suspend fun getGroupKey(
        groupId: String,
        groupOwnerId: String,
        keyVersion: Long? = null
    ): GroupMasterKey {
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
            ?: return GroupMasterKey.Error.Missing

        val ownerPublicKey = when(val ownerPublicKey = getMemberPublicKey(groupId, memberUserId = groupOwnerId)) {
            is GroupMemberMainPublicKey.Error -> {
                return GroupMasterKey.Error.InvalidOwnerPublicKey(ownerPublicKey)
            }
            is GroupMemberMainPublicKey.Valid -> { ownerPublicKey.key }
        }

        val myPreKey = when(val myPreKey = getMyPreKeyForGroup(groupId)) {
            is UserGroupPersonalKeyPair.Error -> {
                return GroupMasterKey.Error.InvalidPersonalKeyPair(myPreKey)
            }
            is UserGroupPersonalKeyPair.Valid -> { myPreKey.key }
        }

        val groupKey = try {
            cryptoCore.decryptKeyWithKeyPair(
                key = keyRaw.encryptedGroupKey,
                myPrivateKey = myPreKey.privateKey,
                publicKey = ownerPublicKey
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group").d("Invalid group key for group $groupId")
            return GroupMasterKey.Error.Invalid
        }

        return GroupMasterKey.Valid(groupKey, keyRaw.version)
    }

    suspend fun getAllGroupKeys(groupId: String): List<GroupMasterKey.Valid> {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return emptyList()
        val ownerPublicKey = getMemberPublicKey(groupId, memberUserId = groupOwnerId).key ?: return emptyList()
        val myPreKey = getMyPreKeyForGroup(groupId).key ?: return emptyList()
        val keys = getGroupKeys().filter { it.groupId == groupId }

        return keys.map {
            val groupKey = cryptoCore.decryptKeyWithKeyPair(
                key = it.encryptedGroupKey,
                myPrivateKey = myPreKey.privateKey,
                publicKey = ownerPublicKey
            )

            GroupMasterKey.Valid(groupKey, it.version)
        }
    }

    suspend fun getAllEncryptedMemberPublicKeys(groupId: String): List<String> {
       return getMemberKeys().filter { it.groupId == groupId }.map { it.encryptedPublicKey }
    }

    suspend fun getMemberKey(
        groupId: String,
        memberUserId: String
    ): GroupMemberSenderKey {
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
            ?: return GroupMemberSenderKey.Error.MissingKeys

        if(keyRaw.encryptedSenderKey.isNullOrBlank()) {
            logg.tag("tag_crypto_key_group_member").w("No keys available for user $memberUserId in group $groupId")
            return GroupMemberSenderKey.Error.MissingSenderKey
        }

        val memberPublicKey = when(val memberPublicKey = getMemberPublicKey(keyRaw)) {
            is GroupMemberMainPublicKey.Error -> {
                return GroupMemberSenderKey.Error.InvalidOwnerPublicKey(memberPublicKey)
            }
            is GroupMemberMainPublicKey.Valid -> { memberPublicKey.key }
        }

        val myPreKey = when(val myPreKey = getMyPreKeyForGroup(groupId)) {
            is UserGroupPersonalKeyPair.Error -> {
                return GroupMemberSenderKey.Error.InvalidPersonalKeyPair(myPreKey)
            }
            is UserGroupPersonalKeyPair.Valid -> { myPreKey.key }
        }

        val memberKey = try {
            cryptoCore.decryptKeyWithKeyPair(
                key = keyRaw.encryptedSenderKey,
                myPrivateKey = myPreKey.privateKey,
                publicKey = memberPublicKey
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").d("Invalid member key for group $groupId & member $memberUserId")
            return GroupMemberSenderKey.Error.Invalid
        }

        return GroupMemberSenderKey.Valid(memberKey, keyRaw.version)
    }

    suspend fun getMemberPublicKey(
        groupId: String,
        memberUserId: String
    ): GroupMemberMainPublicKey {
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
            ?: return GroupMemberMainPublicKey.Error.Missing

        return getMemberPublicKey(keyRaw)
    }

    private suspend fun getMemberPublicKey(
        keyRaw: GroupMemberCryptoKeyDataRaw
    ): GroupMemberMainPublicKey {
        val currentUserId = userRepository.cachedUserId

        // always return own public key from secure storage but ensure no one tampered with key from server
        val identityKey = if (keyRaw.ownerUserId == currentUserId) {
            keyRaw.ownerIdentityKey
                .takeIf { it == userKeyRepository.latestKeyPair().publicKey }
                .also {
                    if (it == null) logg.tag("tag_crypto_key_group_member")
                        .w("User ${keyRaw.ownerUserId} own identity key do not match in group ${keyRaw.groupId}")
                } ?: return GroupMemberMainPublicKey.Error.WrongIdentity
        } else {
            keyRaw.ownerIdentityKey
        }

        val myGroupKeyPair = when(val myPreKey = getMyPreKeyForGroup(keyRaw.groupId)) {
            is UserGroupPersonalKeyPair.Error -> {
                return GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair(myPreKey)
            }
            is UserGroupPersonalKeyPair.Valid -> myPreKey.key
        }

        val kxPublicKey = try {
            cryptoCore.decryptDataWithKeyPair(
                data = keyRaw.encryptedPublicKey,
                publicKey = identityKey,
                myPrivateKey = myGroupKeyPair.privateKey
            ).let { key ->
                // always return own public key from secure storage but ensure no one tampered with key from server
                if(keyRaw.ownerUserId == currentUserId) {
                    myGroupKeyPair.publicKey.takeIf { it == key }.also {
                        if(it == null) logg.tag("tag_crypto_key_group_member").w("User ${keyRaw.ownerUserId} own public key does not match in group ${keyRaw.groupId}")
                    } ?: return GroupMemberMainPublicKey.Error.WrongPublicKey
                } else {
                    key
                }
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").w("Can not decrypt user ${keyRaw.ownerUserId} public key in group ${keyRaw.groupId}")
            return GroupMemberMainPublicKey.Error.InvalidPublicKey
        }

        return GroupMemberMainPublicKey.Valid(kxPublicKey)
    }

    suspend fun getMyPreKeyForGroup(
        groupId: String
    ): UserGroupPersonalKeyPair {
        val allKeys = getPreKeys()
        val key = allKeys.firstOrNull { it.groupId == groupId }
            ?.encryptedUserKeys
            ?: return UserGroupPersonalKeyPair.Error.Missing

        val keyPair = try {
            cryptoCore.decryptDataWithKey(
                data = key,
                key = userKeyRepository.personalKey
            ).let {
                BaseRemoteModule.formatter.decodeFromString<SimpleKeyPairData>(it)
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").d("Invalid pre key for group $groupId")
            return UserGroupPersonalKeyPair.Error.Invalid
        }

        return UserGroupPersonalKeyPair.Valid(keyPair)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        cachedGroupKeys.value = cachedGroupKeys.value.filter { it.groupId != groupId }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.groupId != groupId }
        cachedPreKeys.value = cachedPreKeys.value.filter { it.groupId != groupId }
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
        cachedPreKeys.value = emptyList()
        local.deleteAllKeys()
    }
}
