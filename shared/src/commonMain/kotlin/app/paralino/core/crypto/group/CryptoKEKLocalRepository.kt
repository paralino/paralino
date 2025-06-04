package app.paralino.core.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberIdentityKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.keys.LocalGroupDeviceKeysEntity
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.BaseRemoteModule

class CryptoKEKLocalRepository(
    private val local: CryptoKeyLocalDataSource,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val cryptoCore: CryptoCore,
    private val appPrefs: AppPrefs,
    private val appBuildConfig: AppBuildConfig
) : BaseRepository() {

    suspend fun getGroupKey(groupId: String, keyVersion: Long?): GroupMasterKey {
        val groupOwnerId = local.getGroupOwnerId(groupId) ?: return GroupMasterKey.Error.Missing
        return getGroupKey(groupId, groupOwnerId, keyVersion)
    }

    suspend fun getGroupKey(
        groupId: String,
        groupOwnerId: String,
        keyVersion: Long?
    ): GroupMasterKey {
        val allGroupKeys = local.getGroupKeys()
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

        val groupKey = try {
            if (keyRaw != null) {
                if (keyVersion == null && keyRaw.ownerUserId != groupOwnerId) {
                    // if passed key version is null, we want the latest key
                    // latest key should always be current group owner key
                    return GroupMasterKey.Error.InvalidOwnerPublicKey(GroupMemberMainPublicKey.Error.WrongIdentity)
                } else {
                    val ownerPublicKey =
                        when (val ownerPublicKey = getMemberPublicKey(groupId, memberUserId = keyRaw.ownerUserId)) {
                            is GroupMemberMainPublicKey.Error -> {
                                return GroupMasterKey.Error.InvalidOwnerPublicKey(ownerPublicKey)
                            }

                            is GroupMemberMainPublicKey.Valid -> {
                                ownerPublicKey
                            }
                        }

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
                }
            } else {
                // Try to find matching key
                allGroupKeys
                    .sortedByDescending { it.groupKeyVersion }
                    .firstNotNullOfOrNull { key ->
                        if (keyVersion == null && key.ownerUserId != groupOwnerId) {
                            // if passed key version is null, we want the latest key
                            // latest key should always be current group owner key
                            null
                        } else {
                            try {
                                val ownerPublicKey = getMemberPublicKey(
                                    groupId = groupId,
                                    memberUserId = key.ownerUserId
                                ) as GroupMemberMainPublicKey.Valid
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
        val keys = local.getGroupKeys().filter { it.groupId == groupId }
        return keys.map { key ->
            val ownerPublicKey = getMemberPublicKey(
                groupId = groupId,
                memberUserId = key.ownerUserId
            ) as? GroupMemberMainPublicKey.Valid? ?: return emptyList()

            val myPreKey = getMyPreKeyForGroup(groupId, version = key.memberKeyVersion).key
                ?: return emptyList()

            val groupKey = cryptoCore.decryptKeyWithKeyPair(
                key = key.encryptedGroupKey,
                myPrivateKey = myPreKey.privateKey,
                publicKey = ownerPublicKey.membershipKey
            )

            GroupMasterKey.Valid(groupKey, key.groupKeyVersion)
        }
    }

    suspend fun getAllEncryptedMembershipPublicKeys(groupId: String): List<String> {
        return local.getMemberKeys().filter { it.groupId == groupId }.map { it.signedMembershipPublicKey }
    }

    suspend fun hasGroupKeyByVersion(groupId: String, groupKeyVersion: Long): Boolean {
        return local.getGroupKeys().any {
            it.groupId == groupId && it.groupKeyVersion == groupKeyVersion
        }
    }

    suspend fun hasDeviceKeyByVersion(locationId: String, deviceKeyVersion: Long): Boolean {
        return local.getGroupDeviceKeys().any {
            it.locationId == locationId && it.deviceKeyVersion == deviceKeyVersion
        }
    }

    suspend fun hasMemberKeyByVersion(memberId: String, memberKeyVersion: Long): Boolean {
        return local.getMemberKeys().any {
            it.id == memberId && it.memberKeyVersion == memberKeyVersion
        }
    }

    suspend fun hasPersonalGroupKeyByVersion(groupId: String, version: Long): Boolean {
        return local.getPersonalGroupKeys().any {
            it.groupId == groupId && it.personalGroupKeyVersion == version
        }
    }

    suspend fun getMemberSenderKey(
        groupId: String,
        memberUserId: String,
        locationId: String
    ): GroupMemberSenderKey {
        val currentUserId = appPrefs.userId
        val allMemberKeys = local.getGroupDeviceKeys()
        val keyRaw = allMemberKeys.firstOrNull {
            it.groupId == groupId
                    && it.ownerUserId == memberUserId
                    && it.memberUserId == currentUserId
                    && it.locationId == locationId
        }
            ?: let {
                logg.tag("tag_crypto_key_group_member")
                    .d("member sender key not found for group $groupId & member $memberUserId, key count: ${allMemberKeys.size}")
                return GroupMemberSenderKey.Error.MissingKeys
            }

        if (memberUserId == appPrefs.userId) {
            val key = local.getLocalGroupSenderKey(groupId = groupId)
                ?.takeIf { key ->
                    key.locationId == locationId
                            && keyRaw.memberKeyVersion == key.memberKeyVersion
                            && keyRaw.deviceKeyVersion == key.deviceKeyVersion
                }
                ?.let { key ->
                    logg.tag("tag_crypto_key_group_member")
                        .d("Try to load current device sender key from encrypted cache")
                    try {
                        GroupMemberSenderKey.Valid(
                            key = cryptoCore.decryptDataWithKey(
                                data = key.encryptedSenderKey,
                                key = userKeyRepository.personalKey(version = key.userKeyVersion).key
                            ),
                            deviceKeyVersion = key.deviceKeyVersion,
                            memberKeyVersion = key.memberKeyVersion
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

            if (key != null) {
                return key
            }
        }

        return parseMemberSenderKey(keyRaw)
    }

    private suspend fun parseMemberSenderKey(
        keyRaw: GroupDeviceCryptoKeyRaw
    ): GroupMemberSenderKey {

        if (keyRaw.encryptedSenderKey.isBlank()) {
            logg.tag("tag_crypto_key_group_member")
                .w("No keys available for user ${keyRaw.memberUserId} in group ${keyRaw.groupId}")
            return GroupMemberSenderKey.Error.MissingSenderKey
        }

        val memberPublicKey = when (val memberPublicKey =
            getMemberPublicKey(groupId = keyRaw.groupId, memberUserId = keyRaw.ownerUserId)) {
            is GroupMemberMainPublicKey.Error -> {
                return GroupMemberSenderKey.Error.InvalidOwnerPublicKey(memberPublicKey)
            }

            is GroupMemberMainPublicKey.Valid -> {
                memberPublicKey
            }
        }

        val myPreKey =
            when (val myPreKey = getMyPreKeyForGroup(keyRaw.groupId, keyRaw.memberKeyVersion)) {
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
                .w("Invalid sender key for group ${keyRaw.groupId} & member ${keyRaw.memberUserId} (${e.message})")
            return GroupMemberSenderKey.Error.Invalid
        }

        if (keyRaw.ownerUserId == appPrefs.userId && appBuildConfig.appHost == AppHost.MAIN_APP) {
            if (keyRaw.locationId == local.getCurrentDeviceLocationId(groupId = keyRaw.groupId)) {
                logg.tag("tag_crypto_key_group_member")
                    .d("Save current device sender key to encrypted cache")
                val userKey = userKeyRepository.activePersonalKeyWithVersion()
                val localKey = LocalGroupDeviceKeysEntity(
                    id = keyRaw.groupId,
                    locationId = keyRaw.locationId,
                    memberKeyVersion = keyRaw.memberKeyVersion,
                    deviceKeyVersion = keyRaw.deviceKeyVersion,
                    userKeyVersion = userKey.version,
                    encryptedSenderKey = cryptoCore.encryptDataWithKey(
                        data = memberKey,
                        key = userKey.key
                    )
                )
                local.saveLocalGroupSenderKey(key = localKey)
            }
        }

        return GroupMemberSenderKey.Valid(
            key = memberKey,
            deviceKeyVersion = keyRaw.deviceKeyVersion,
            memberKeyVersion = keyRaw.memberKeyVersion
        )
    }

    suspend fun getGroupDeviceKeys(): List<GroupDeviceCryptoKeyRaw> {
        return local.getGroupDeviceKeys()
    }

    suspend fun getMemberPublicKey(
        groupId: String,
        memberUserId: String
    ): GroupMemberMainPublicKey {
        val allMemberKeys = local.getMemberKeys()
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
        val currentUserId = appPrefs.userId

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
                            .w("User ${keyRaw.ownerUserId} own membership key does not match in group ${keyRaw.groupId}")
                    } ?: return GroupMemberMainPublicKey.Error.WrongPublicKey
                } else {
                    key
                }
            } ?: return GroupMemberMainPublicKey.Error.UnverifiedPublicKey
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member_public")
                .w("Can not decrypt user ${keyRaw.ownerUserId} membership key in group ${keyRaw.groupId}")
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
                            .w("User ${keyRaw.ownerUserId} own public kx key does not match in group ${keyRaw.groupId}")
                    } ?: return GroupMemberMainPublicKey.Error.WrongPublicKey
                } else {
                    key
                }
            } ?: return GroupMemberMainPublicKey.Error.UnverifiedPublicKey
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member_public")
                .w("Can not decrypt user ${keyRaw.ownerUserId} public kx key in group ${keyRaw.groupId}")
            return GroupMemberMainPublicKey.Error.InvalidPublicKey
        }

        return GroupMemberMainPublicKey.Valid(
            key = kxPublicKey,
            signedKey = keyRaw.signedMemberKxPublicKey,
            memberKeyVersion = keyRaw.memberKeyVersion,
            membershipKey = membershipPublicKey,
            signedMemberShipKey = keyRaw.signedMembershipPublicKey,
            identityKey = identityKey
        )
    }

    suspend fun getMemberIdentityKey(
        keyRaw: GroupMemberCryptoKeyDataRaw
    ): GroupMemberIdentityKey {
        val currentUserId = appPrefs.userId
        // always return own identity key from secure storage but ensure no one tampered with key from server
        val identityKey = if (keyRaw.ownerUserId == currentUserId) {
            keyRaw.ownerIdentityKey
                .takeIf {
                    try {
                        it == userKeyRepository.signatureKeyPair().publicKey
                    } catch (e: Exception) {
                        return GroupMemberIdentityKey.Error.MissingOwnerSignatureKeys
                    }
                }
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
        val allKeys = local.getPersonalGroupKeys()
            .filter { it.groupId == groupId }
        val keyRaw = allKeys
            .let { keys ->
                val matchingKey = if (version != null) {
                    // if key version is passed get matching key
                    keys.takeIf { it.count { it.personalGroupKeyVersion == version } == 1 }
                        ?.firstOrNull { it.personalGroupKeyVersion == version }
                } else {
                    // otherwise get latest key
                    keys.maxByOrNull { it.personalGroupKeyVersion }
                }

//                logg.tag("tag_crypto_key_group").d("group keys count: ${allKeys.size} - ${keys.size} for group $groupId, with version ($version) match: ${matchingKey != null}")

                matchingKey
            }

        val keyPair = try {
            if (keyRaw != null) {
                cryptoCore.decryptDataWithKey(
                    data = keyRaw.encryptedUserKeys,
                    key = userKeyRepository.personalKey(version = keyRaw.userKeyVersion).key
                ).let {
                    BaseRemoteModule.formatter.decodeFromString<SimpleKeyPairData>(it)
                } to keyRaw.personalGroupKeyVersion
            } else {
                allKeys
                    .sortedByDescending { it.personalGroupKeyVersion }
                    .firstNotNullOfOrNull { key ->
                        try {
                            cryptoCore.decryptDataWithKey(
                                data = key.encryptedUserKeys,
                                key = userKeyRepository.personalKey(version = key.userKeyVersion).key
                            ).let {
                                BaseRemoteModule.formatter.decodeFromString<SimpleKeyPairData>(it)
                            } to key.personalGroupKeyVersion
                        } catch (e: Exception) {
                            null
                        }
                    } ?: return UserGroupPersonalKeyPair.Error.Missing
            }
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_group_member").d(
                message = "Invalid pre key for group $groupId",
                t = e
            )
            return UserGroupPersonalKeyPair.Error.Invalid
        }

        return UserGroupPersonalKeyPair.Valid(keyPair.first, keyPair.second)
    }

    suspend fun getAllActiveMemberKeyVersions(groupId: String): List<Long> {
        val groupDeviceKeys = local.getGroupDeviceKeys().filter {
            // group device keys created for me
            it.groupId == groupId && it.memberUserId == appPrefs.userId
        }.map { it.memberKeyVersion }
        val groupKeys = local.getGroupKeys().filter {
            // group master keys created for me
            it.groupId == groupId && it.memberUserId == appPrefs.userId
        }.map { it.memberKeyVersion }
        val memberKeys = local.getMemberKeys().filter {
            // member keys
            it.groupId == groupId && it.ownerUserId == appPrefs.userId
        }.map { it.memberKeyVersion }
        return (groupDeviceKeys + groupKeys + memberKeys).distinct()
    }

    suspend fun updateIntegrity(
        groupId: String,
        integrity: String,
        safetySignature: String,
        userKeyVersion: Long
    ) {
        local.updateGroupDeviceIntegrity(
            groupId = groupId,
            integrity = integrity,
            safetySignature = safetySignature,
            userKeyVersion = userKeyVersion
        )
    }

    suspend fun saveGroupHistoryKey(
        groupId: String,
        userKeyVersion: Long,
        historyKeyVersion: String,
        encryptedHistoryKey: String,
        isSynced: Boolean,
    ) {
        local.saveGroupHistoryKey(
            groupId = groupId,
            userKeyVersion = userKeyVersion,
            historyKeyVersion = historyKeyVersion,
            encryptedHistoryKey = encryptedHistoryKey,
            isSynced = isSynced
        )
    }

    suspend fun getDeviceIntegrityForGroup(groupId: String): GroupDeviceIntegrityEntity? {
        return local.getGroupDeviceIntegrity(groupId)
    }

    suspend fun deleteAllKeysForGroup(groupId: String) {
        local.deleteAllKeysForGroup(groupId)
    }

    suspend fun deleteKeysForUser(groupId: String, userId: String) {
        local.deleteKeysForUser(groupId, userId)
    }

    suspend fun clearCachedKeys() {
        local.clearCache()
    }
}
