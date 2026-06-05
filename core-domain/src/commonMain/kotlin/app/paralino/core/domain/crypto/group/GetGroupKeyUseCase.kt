package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMasterKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.logg

class GetGroupKeyUseCase(
    private val cryptoCore: CryptoCore,
    private val local: CryptoKeyLocalDataSource,
    private val getMemberPublicKey: GetMemberPublicKeyUseCase,
    private val getPersonalGroupKey: GetPersonalGroupKeyUseCase
) {
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
                        when (
                            val ownerPublicKey = getMemberPublicKey.getMemberPublicKey(
                                groupId = groupId,
                                memberUserId = keyRaw.ownerUserId
                            )
                        ) {
                            is GroupMemberMainPublicKey.Error -> {
                                return GroupMasterKey.Error.InvalidOwnerPublicKey(ownerPublicKey)
                            }

                            is GroupMemberMainPublicKey.Valid -> {
                                ownerPublicKey
                            }
                        }

                    val myPreKey = when (
                        val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                            groupId = groupId,
                            version = keyRaw.memberKeyVersion,
                            includeArchivedKeys = false
                        )
                    ) {
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
                                val ownerPublicKey = getMemberPublicKey.getMemberPublicKey(
                                    groupId = groupId,
                                    memberUserId = key.ownerUserId
                                ) as GroupMemberMainPublicKey.Valid
                                val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                                    groupId = groupId,
                                    version = key.memberKeyVersion,
                                    includeArchivedKeys = false
                                ).key
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
        val keys = local.getGroupKeys()
            .filter { it.groupId == groupId }

        return keys.map { key ->
            val ownerPublicKey = getMemberPublicKey.getMemberPublicKey(
                groupId = groupId,
                memberUserId = key.ownerUserId
            ) as? GroupMemberMainPublicKey.Valid? ?: return emptyList()

            val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                groupId = groupId,
                version = key.memberKeyVersion,
                includeArchivedKeys = false
            ).key ?: return emptyList()

            val groupKey = cryptoCore.decryptKeyWithKeyPair(
                key = key.encryptedGroupKey,
                myPrivateKey = myPreKey.privateKey,
                publicKey = ownerPublicKey.membershipKey
            )

            GroupMasterKey.Valid(groupKey, key.groupKeyVersion)
        }
    }
}