package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.GroupMemberSenderKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.keys.LocalGroupDeviceKeysEntity
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.AppHost
import app.paralino.core.models.raw.GroupDeviceCryptoKeyRaw

class GetMemberSenderKeyUseCase(
    private val cryptoCore: CryptoCore,
    private val getMemberPublicKey: GetMemberPublicKeyUseCase,
    private val getPersonalGroupKey: GetPersonalGroupKeyUseCase,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val local: CryptoKeyLocalDataSource,
    private val appPrefs: AppPrefs,
    private val appBuildConfig: AppBuildConfig
) {
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

        val memberPublicKey = when (
            val memberPublicKey = getMemberPublicKey.getMemberPublicKey(
                groupId = keyRaw.groupId,
                memberUserId = keyRaw.ownerUserId
            )
        ) {
            is GroupMemberMainPublicKey.Error -> {
                return GroupMemberSenderKey.Error.InvalidOwnerPublicKey(memberPublicKey)
            }

            is GroupMemberMainPublicKey.Valid -> {
                memberPublicKey
            }
        }

        val myPreKey = when (
            val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                groupId = keyRaw.groupId,
                version = keyRaw.memberKeyVersion,
                includeArchivedKeys = false
            )
        ) {
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
}