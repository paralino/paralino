package app.paralino.core.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberIdentityKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.data.db.GroupDeviceIntegrityEntity
import app.paralino.core.logg
import app.paralino.core.models.raw.GroupCryptoKeyDataRaw
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.models.raw.PersonalGroupKeyRaw
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
    private var cachedGroupDeviceKeys = MutableStateFlow<List<GroupDeviceCryptoKeyRaw>>(emptyList())
    private var cachedPersonalGroupKeys = MutableStateFlow<List<PersonalGroupKeyRaw>>(emptyList())

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

    suspend fun getGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return cachedGroupDeviceKeys.value.ifEmpty {
            local.getAllGroupDeviceKeys()
                .firstOrNull()
                ?.also { cachedGroupDeviceKeys.value = it }
                ?: emptyList()
        }
    }

    suspend fun getPersonalGroupKeys(): List<PersonalGroupKeyRaw> {
        return cachedPersonalGroupKeys.value.ifEmpty {
            local.getAllPersonalGroupKeys()
                .firstOrNull()
                ?.also { cachedPersonalGroupKeys.value = it }
                ?: emptyList()
        }
    }


    suspend fun refreshAllKeys() {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key").w("refreshing group keys")
            val groupKeys = remote.getAllGroupKeys()
            logg.tag("tag_crypto_key").w("refreshing member keys")
            val memberKeys = remote.getAllGroupMemberKeys()
            logg.tag("tag_crypto_key").w("refreshing device keys")
            val deviceKeys = remote.getAllGroupDeviceKeys()
            logg.tag("tag_crypto_key").w("refreshing pre keys")
            val preKeys = remote.getAllPersonalGroupKeys()

            cachedGroupKeys.value = groupKeys
            cachedMemberKeys.value = memberKeys
            cachedGroupDeviceKeys.value = deviceKeys
            cachedPersonalGroupKeys.value = preKeys

            local.recreateAllGroupKeys(groupKeys, memberKeys, deviceKeys, preKeys)

            logg.tag("tag_crypto_key")
                .w("updated group (${groupKeys.size}), member (${memberKeys.size}), device (${deviceKeys.size}), pre (${preKeys.size}) keys")
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
            val preKeys = remote.getAllPersonalGroupKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member")
                .w("updated member (${memberKeys.size}), pre (${preKeys.size}) keys")

            cachedMemberKeys.value = emptyList()
            cachedPersonalGroupKeys.value = emptyList()
            local.recreateMemberKeysForGroup(groupId, memberKeys, preKeys)
        }
    }

    suspend fun refreshDeviceKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group_member").w("refreshing only group device key")
            val deviceKeys = remote.getAllGroupDeviceKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group_member")
                .w("updated member device (${deviceKeys.size}) keys")

            cachedGroupDeviceKeys.value = emptyList()
            local.recreateDeviceKeysForGroup(groupId, deviceKeys)
        }
    }

    suspend fun refreshKeysForGroup(groupId: String) {
        triggerRemoteOnly {
            logg.tag("tag_crypto_key_group").d("refreshing group key")
            val groupKeys = remote.getGroupKeyForGroup(groupId)
            logg.tag("tag_crypto_key_group").d("refreshing group member key")
            val memberKeys = remote.getAllGroupMemberKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group").d("refreshing group device key")
            val deviceKeys = remote.getAllGroupDeviceKeysForGroup(groupId)
            logg.tag("tag_crypto_key_group").w("refreshing group pre keys")
            val preKeys = remote.getAllPersonalGroupKeysForGroup(groupId)

            logg.tag("tag_crypto_key_group")
                .d("Got new group (${groupKeys.size}), member (${memberKeys.size}), device (${deviceKeys.size}), pre (${preKeys.size}) for group $groupId")

            cachedGroupKeys.value = emptyList()
            cachedMemberKeys.value = emptyList()
            cachedGroupDeviceKeys.value = emptyList()
            cachedPersonalGroupKeys.value = emptyList()
            local.recreateGroupKeyForGroup(groupId, groupKeys, memberKeys, deviceKeys, preKeys)

            logg.tag("tag_crypto_key_group").d("updated group and member keys for group")
        }
    }

    suspend fun getLatestGroupKeyVersion(groupId: String): Long? {
        return getGroupKeys().filter { it.groupId == groupId }
            .maxByOrNull { it.groupKeyVersion }?.groupKeyVersion
    }

    suspend fun getGroupKey(groupId: String, keyVersion: Long?): GroupMasterKey {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return GroupMasterKey.Error.Missing
        return getGroupKey(groupId, groupOwnerId, keyVersion)
    }

    suspend fun getGroupKey(
        groupId: String,
        groupOwnerId: String,
        keyVersion: Long?
    ): GroupMasterKey {
        val allGroupKeys = getGroupKeys()
            .filter { it.groupId == groupId }
        val keyRaw = allGroupKeys
            .let { keys ->
                val matchingKey = if (keyVersion != null) {
                    // if key version is passed get matching key
                    // if for any reason there are multiple keys with same version - ignore
                    // step below will try to find a proper key
                    keys.takeIf { it.count { it.groupKeyVersion == keyVersion } == 1 }
                        ?.firstOrNull { it.groupKeyVersion == keyVersion }
                } else {
                    // otherwise get latest key
                    keys.maxByOrNull { it.groupKeyVersion }
                }

//                logg.tag("tag_crypto_key_group").d("group keys count: ${allGroupKeys.size} - ${keys.size} for group $groupId, with version ($keyVersion) match: ${matchingKey != null}")

                matchingKey
            }

        val ownerPublicKey =
            when (val ownerPublicKey = getMemberPublicKey(groupId, memberUserId = groupOwnerId)) {
                is GroupMemberMainPublicKey.Error -> {
                    return GroupMasterKey.Error.InvalidOwnerPublicKey(ownerPublicKey)
                }

                is GroupMemberMainPublicKey.Valid -> {
                    ownerPublicKey
                }
            }

        val groupKey = try {
            if (keyRaw != null) {
                val myPreKey = when (val myPreKey =
                    getMyPreKeyForGroup(groupId, version = keyRaw.memberKeyVersion)) {
                    is UserGroupPersonalKeyPair.Error -> {
                        return GroupMasterKey.Error.InvalidPersonalKeyPair(myPreKey)
                    }

                    is UserGroupPersonalKeyPair.Valid -> {
                        myPreKey.key
                    }
                }
                cryptoCore.decryptKeyWithKeyPair(
                    key = keyRaw.encryptedGroupKey,
                    myPrivateKey = myPreKey.privateKey,
                    publicKey = ownerPublicKey.membershipKey
                ) to keyRaw.groupKeyVersion
            } else {
                // Try to find matching key
                allGroupKeys
                    .sortedByDescending { it.groupKeyVersion }
                    .firstNotNullOfOrNull { key ->
                        try {
                            val myPreKey =
                                getMyPreKeyForGroup(groupId, version = key.memberKeyVersion).key
                            cryptoCore.decryptKeyWithKeyPair(
                                key = key.encryptedGroupKey,
                                myPrivateKey = myPreKey!!.privateKey,
                                publicKey = ownerPublicKey.membershipKey
                            ) to key.groupKeyVersion
                        } catch (e: Exception) {
                            null
                        }
                    } ?: return GroupMasterKey.Error.Missing
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group").d("Invalid group key for group $groupId")
            return GroupMasterKey.Error.Invalid
        }

        return GroupMasterKey.Valid(groupKey.first, groupKey.second)
    }

    suspend fun getAllGroupKeys(groupId: String): List<GroupMasterKey.Valid> {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return emptyList()
        val ownerPublicKey =
            getMemberPublicKey(groupId, memberUserId = groupOwnerId) as? GroupMemberMainPublicKey.Valid? ?: return emptyList()
        val keys = getGroupKeys().filter { it.groupId == groupId }

        return keys.map {
            val myPreKey = getMyPreKeyForGroup(groupId, version = it.memberKeyVersion).key ?: return emptyList()
            val groupKey = cryptoCore.decryptKeyWithKeyPair(
                key = it.encryptedGroupKey,
                myPrivateKey = myPreKey.privateKey,
                publicKey = ownerPublicKey.membershipKey
            )

            GroupMasterKey.Valid(groupKey, it.groupKeyVersion)
        }
    }

    suspend fun getAllEncryptedMembershipPublicKeys(groupId: String): List<String> {
        return getMemberKeys().filter { it.groupId == groupId }.map { it.signedMembershipPublicKey }
    }

    suspend fun getMemberSenderKey(
        groupId: String,
        memberUserId: String,
        locationId: String
    ): GroupMemberSenderKey {
        val currentUserId = userRepository.cachedUserId
        val allMemberKeys = getGroupDeviceKeys()
        val keyRaw = allMemberKeys
            // filter keys that memberUserId created for current user
            .filter {
                it.groupId == groupId
                        && it.ownerUserId == memberUserId
                        && it.memberUserId == currentUserId
                        && it.locationId == locationId
            }
//            .let { keys ->
//                logg.tag("tag_crypto_key_group_member").d("member keys count: ${allMemberKeys.size} - ${keys.size} filtered for group $groupId & member $memberUserId")
//                keys
//            }
            .firstOrNull()
            ?: let {
                logg.tag("tag_crypto_key_group_member")
                    .d("member sender key not found for group $groupId & member $memberUserId, key count: ${allMemberKeys.size}")
                return GroupMemberSenderKey.Error.MissingKeys
            }

        if (keyRaw.encryptedSenderKey.isBlank()) {
            logg.tag("tag_crypto_key_group_member")
                .w("No keys available for user $memberUserId in group $groupId")
            return GroupMemberSenderKey.Error.MissingSenderKey
        }

        val memberPublicKey = when (val memberPublicKey =
            getMemberPublicKey(groupId = groupId, memberUserId = memberUserId)) {
            is GroupMemberMainPublicKey.Error -> {
                return GroupMemberSenderKey.Error.InvalidOwnerPublicKey(memberPublicKey)
            }

            is GroupMemberMainPublicKey.Valid -> {
                memberPublicKey
            }
        }

        val myPreKey = when (val myPreKey = getMyPreKeyForGroup(groupId, keyRaw.memberKeyVersion)) {
            is UserGroupPersonalKeyPair.Error -> {
                return GroupMemberSenderKey.Error.InvalidPersonalKeyPair(myPreKey)
            }

            is UserGroupPersonalKeyPair.Valid -> {
                myPreKey
            }
        }

        val memberKey = try {
            cryptoCore.decryptKeyWithKeyPair(
                key = keyRaw.encryptedSenderKey,
                myPrivateKey = myPreKey.key.privateKey,
                publicKey = memberPublicKey.membershipKey
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member")
                .w("Invalid member key for group $groupId & member $memberUserId (${e.message})")
            return GroupMemberSenderKey.Error.Invalid
        }

        return GroupMemberSenderKey.Valid(memberKey, keyRaw.memberKeyVersion)
    }

    suspend fun getMemberPublicKey(
        groupId: String,
        memberUserId: String
    ): GroupMemberMainPublicKey {
        val allMemberKeys = getMemberKeys()
        val keyRaw = allMemberKeys
            // filter keys that memberUserId created for current user
            .filter { it.groupId == groupId && it.ownerUserId == memberUserId }
//            .let { keys ->
//                logg.tag("tag_crypto_key_group_member_public").d("member keys count: ${allMemberKeys.size} - ${keys.size} filtered for group $groupId & member $memberUserId")
//                keys
//            }
            .maxByOrNull { it.memberKeyVersion ?: -1 }
            ?: let {
                logg.tag("tag_crypto_key_group_member_public")
                    .d("member public key not found for group $groupId & member $memberUserId, keys count: ${allMemberKeys.size}")
                return GroupMemberMainPublicKey.Error.Missing
            }

        return getMemberPublicKey(keyRaw)
    }

    private suspend fun getMemberPublicKey(
        keyRaw: GroupMemberCryptoKeyDataRaw
    ): GroupMemberMainPublicKey {
        val currentUserId = userRepository.cachedUserId

        val identityKey = getMemberIdentityKey(keyRaw)

        if (identityKey !is GroupMemberIdentityKey.Valid)
            return GroupMemberMainPublicKey.Error.WrongIdentity

        val membershipPublicKey = try {
            cryptoCore.verifySignature(
                data = keyRaw.signedMembershipPublicKey,
                publicKey = identityKey.key
            )?.let { key ->
                // always return own public key from secure storage but ensure no one tampered with key from server
                if (keyRaw.ownerUserId == currentUserId) {
                    val myGroupKeyPair =
                        when (val myPreKey = getMyPreKeyForGroup(keyRaw.groupId, version = 0)) {
                            is UserGroupPersonalKeyPair.Error -> {
                                return GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair(
                                    myPreKey
                                )
                            }

                            is UserGroupPersonalKeyPair.Valid -> myPreKey.key
                        }
                    myGroupKeyPair.publicKey.takeIf { it == key }.also {
                        if (it == null) logg.tag("tag_crypto_key_group_member_public")
                            .w("User ${keyRaw.ownerUserId} own public key does not match in group ${keyRaw.groupId}")
                    } ?: return GroupMemberMainPublicKey.Error.WrongPublicKey
                } else {
                    key
                }
            } ?: return GroupMemberMainPublicKey.Error.UnverifiedPublicKey
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member_public")
                .w("Can not decrypt user ${keyRaw.ownerUserId} public key in group ${keyRaw.groupId}")
            return GroupMemberMainPublicKey.Error.InvalidPublicKey
        }

        val kxPublicKey = try {
            cryptoCore.verifySignature(
                data = keyRaw.signedMemberKxPublicKey,
                publicKey = identityKey.key
            )?.let { key ->
                // always return own public key from secure storage but ensure no one tampered with key from server
                if (keyRaw.ownerUserId == currentUserId) {
                    val myGroupKeyPair = when (val myPreKey =
                        getMyPreKeyForGroup(keyRaw.groupId, keyRaw.memberKeyVersion)) {
                        is UserGroupPersonalKeyPair.Error -> {
                            return GroupMemberMainPublicKey.Error.InvalidPersonalKeyPair(myPreKey)
                        }

                        is UserGroupPersonalKeyPair.Valid -> myPreKey.key
                    }
                    myGroupKeyPair.publicKey.takeIf { it == key }.also {
                        if (it == null) logg.tag("tag_crypto_key_group_member_public")
                            .w("User ${keyRaw.ownerUserId} own public key does not match in group ${keyRaw.groupId}")
                    } ?: return GroupMemberMainPublicKey.Error.WrongPublicKey
                } else {
                    key
                }
            } ?: return GroupMemberMainPublicKey.Error.UnverifiedPublicKey
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member_public")
                .w("Can not decrypt user ${keyRaw.ownerUserId} public key in group ${keyRaw.groupId}")
            return GroupMemberMainPublicKey.Error.InvalidPublicKey
        }

        return GroupMemberMainPublicKey.Valid(
            key = kxPublicKey,
            signedKey = keyRaw.signedMemberKxPublicKey,
            version = keyRaw.memberKeyVersion,
            membershipKey = membershipPublicKey,
            signedMemberShipKey = keyRaw.signedMembershipPublicKey,
            identityKey = identityKey
        )
    }

    suspend fun getMemberIdentityKey(
        keyRaw: GroupMemberCryptoKeyDataRaw
    ): GroupMemberIdentityKey {
        val currentUserId = userRepository.cachedUserId
        // always return own identity key from secure storage but ensure no one tampered with key from server
        val identityKey = if (keyRaw.ownerUserId == currentUserId) {
            keyRaw.ownerIdentityKey
                .takeIf { it == userKeyRepository.latestKeyPair().publicKey }
                .also {
                    if (it == null) logg.tag("tag_crypto_key_group_member")
                        .w("User ${keyRaw.ownerUserId} own identity key do not match in group ${keyRaw.groupId}")
                } ?: return GroupMemberIdentityKey.Error.Invalid
        } else {
            keyRaw.ownerIdentityKey
        }
        return GroupMemberIdentityKey.Valid(identityKey)
    }

    suspend fun getMyPreKeyForGroup(
        groupId: String,
        version: Long?
    ): UserGroupPersonalKeyPair {
        val allKeys = getPersonalGroupKeys()
            .filter { it.groupId == groupId }
        val keyRaw = allKeys
            .let { keys ->
                val matchingKey = if (version != null) {
                    // if key version is passed get matching key
                    keys.takeIf { it.count { it.version == version } == 1 }
                        ?.firstOrNull { it.version == version }
                } else {
                    // otherwise get latest key
                    keys.maxByOrNull { it.version }
                }

//                logg.tag("tag_crypto_key_group").d("group keys count: ${allKeys.size} - ${keys.size} for group $groupId, with version ($version) match: ${matchingKey != null}")

                matchingKey
            }

        val keyPair = try {
            if (keyRaw != null) {
                cryptoCore.decryptDataWithKey(
                    data = keyRaw.encryptedUserKeys,
                    key = userKeyRepository.personalKey
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<SimpleKeyPairData>(it)
                } to keyRaw.version
            } else {
                allKeys
                    .sortedByDescending { it.version }
                    .firstNotNullOfOrNull { key ->
                        try {
                            cryptoCore.decryptDataWithKey(
                                data = key.encryptedUserKeys,
                                key = userKeyRepository.personalKey
                            ).let {
                                BaseRemoteModule.formatter.decodeFromString<SimpleKeyPairData>(it)
                            } to key.version
                        } catch (e: Exception) {
                            null
                        }
                    } ?: return UserGroupPersonalKeyPair.Error.Missing
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").d("Invalid pre key for group $groupId")
            return UserGroupPersonalKeyPair.Error.Invalid
        }

        return UserGroupPersonalKeyPair.Valid(keyPair.first, keyPair.second)
    }

    suspend fun updateIntegrity(
        groupId: String,
        integrity: String
    ) {
        local.updateGroupDeviceIntegrity(groupId, integrity)
    }

    suspend fun getDeviceIntegrityForGroup(groupId: String): String? {
        return local.getGroupDeviceIntegrity(groupId)?.integrity
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        cachedGroupKeys.value = cachedGroupKeys.value.filter { it.groupId != groupId }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.groupId != groupId }
        cachedGroupDeviceKeys.value = cachedGroupDeviceKeys.value.filter { it.groupId != groupId }
        cachedPersonalGroupKeys.value =
            cachedPersonalGroupKeys.value.filter { it.groupId != groupId }
        local.deleteAllKeysForGroup(groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        cachedGroupDeviceKeys.value = cachedGroupDeviceKeys.value.filter {
            // remove all keys that were either created by or for userId
            !(it.memberUserId == userId || it.ownerUserId == userId)
        }
        cachedMemberKeys.value = cachedMemberKeys.value.filter { it.ownerUserId != userId }
        local.deleteKeysForUser(groupId, userId)
    }

    suspend fun clearKeys() {
        cachedMemberKeys.value = emptyList()
        cachedGroupKeys.value = emptyList()
        cachedGroupDeviceKeys.value = emptyList()
        cachedPersonalGroupKeys.value = emptyList()
        local.deleteAllKeys()
    }
}
