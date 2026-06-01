package app.paralino.core.domain.crypto.group

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.crypto.UserGroupPersonalKeyPair
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.logg
import app.paralino.core.utils.BaseRemoteModule

class GetPersonalGroupKeyUseCase(
    private val cryptoCore: CryptoCore,
    private val local: CryptoKeyLocalDataSource,
    private val userKeyRepository: CryptoUserKeysLocalSource,
) {

    suspend fun getMyPreKeyForGroup(
        groupId: String,
        version: Long?,
        includeArchivedKeys: Boolean
    ): UserGroupPersonalKeyPair {
        val allKeys = local.getPersonalGroupKeys(
            groupId = groupId,
            includeArchivedKeys = includeArchivedKeys
        )

        val keyRaw = allKeys.let { keys ->
            val matchingKey = if (version != null) {
                // if key version is passed get matching key
                keys.takeIf { it.count { it.personalGroupKeyVersion == version } == 1 }
                    ?.firstOrNull { it.personalGroupKeyVersion == version }
            } else {
                // otherwise get latest key
                keys.maxByOrNull { it.personalGroupKeyVersion }
            }

//          logg.tag("tag_crypto_key_group").d("group keys count: ${allKeys.size} - ${keys.size} for group $groupId, with version ($version) match: ${matchingKey != null}")

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
}