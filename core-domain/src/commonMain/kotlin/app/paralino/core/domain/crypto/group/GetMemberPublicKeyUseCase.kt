package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberIdentityKey
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.models.raw.GroupMemberCryptoKeyDataRaw

class GetMemberPublicKeyUseCase(
    private val cryptoCore: CryptoCore,
    private val getPersonalGroupKey: GetPersonalGroupKeyUseCase,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val local: CryptoKeyLocalDataSource,
    private val appPrefs: AppPrefs
) {

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
                    val myGroupKeyPair = when (
                        val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                            groupId = keyRaw.groupId,
                            version = 0
                        )
                    ) {
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
                    val myGroupKeyPair = when (
                        val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                            groupId = keyRaw.groupId,
                            version = keyRaw.memberKeyVersion
                        )
                    ) {
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

    private suspend fun getMemberIdentityKey(
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
}