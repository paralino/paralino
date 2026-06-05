package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.LocationHistoryKey
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.logg

class GetGroupHistoryKeyUseCase(
    private val cryptoCore: CryptoCore,
    private val getMemberPublicKey: GetMemberPublicKeyUseCase,
    private val getPersonalGroupKey: GetPersonalGroupKeyUseCase,
) {
    suspend fun parseGroupLocationHistoryKey(
        keyRaw: GroupHistoryKeyEntity
    ): LocationHistoryKey {
        if (keyRaw.encryptedHistoryKey.isBlank()) {
            logg.tag("tag_crypto_key_history")
                .w("No history keys available for user ${keyRaw.ownerUserId} in group ${keyRaw.groupId}")
            return LocationHistoryKey.Error.MissingHistoryKey
        }

        val memberPublicKey = when (
            val memberPublicKey = getMemberPublicKey.getMemberPublicKey(groupId = keyRaw.groupId, memberUserId = keyRaw.ownerUserId)
        ) {
            is GroupMemberMainPublicKey.Error -> {
                return LocationHistoryKey.Error.InvalidOwnerPublicKey(memberPublicKey)
            }

            is GroupMemberMainPublicKey.Valid -> {
                memberPublicKey
            }
        }

        val myPreKey =
            when (
                val myPreKey = getPersonalGroupKey.getMyPreKeyForGroup(
                    groupId = keyRaw.groupId,
                    version = keyRaw.memberKeyVersion,
                    includeArchivedKeys = true
                )
            ) {
                is UserGroupPersonalKeyPair.Error -> {
                    return LocationHistoryKey.Error.InvalidPersonalKeyPair(myPreKey)
                }

                is UserGroupPersonalKeyPair.Valid -> {
                    myPreKey
                }
            }

        val historyKey = try {
            cryptoCore.decryptKeyWithKeyPair(
                key = keyRaw.encryptedHistoryKey,
                myPrivateKey = myPreKey.key.privateKey,
                publicKey = memberPublicKey.membershipKey
            )
        } catch (e: Exception) {
            logg.tag("tag_crypto_key_history")
                .w("Invalid history key for group ${keyRaw.groupId} & member ${keyRaw.ownerUserId} (${e.message})")
            return LocationHistoryKey.Error.Invalid
        }

        return LocationHistoryKey.Valid(
            key = historyKey,
            historyKeyVersion = keyRaw.historyKeyVersion,
            memberKeyVersion = keyRaw.memberKeyVersion
        )
    }
}