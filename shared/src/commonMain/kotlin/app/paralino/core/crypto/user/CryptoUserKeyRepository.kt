package app.paralino.core.crypto.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.base64ToData
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.user.CryptoUserKeysRemoteSource
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.flow.Flow

class CryptoUserKeyRepository(
    private val cryptoCore: CryptoCore,
    private val local: ICryptoUserKeyLocalDataSource,
    private val remote: CryptoUserKeysRemoteSource,
) : BaseRepository() {

    val personalKey: Data
        get() {
            return personalKeyOrNull ?: throw MissingUserKeyException(localCache = true)
        }

    val personalKeyOrNull: Data?
        get() {
            return local.getCachedUserKey().takeIf { it.isNotBlank() }
                ?.base64ToData(cryptoCore.hashProvider)
                ?.takeIf { it.content.isNotEmpty() }
        }

    suspend fun latestKeyPair(): AsymmetricCryptoKeyData {
        return local.getKeyPair() ?: throw MissingUserKeyException(localCache = false)
    }

    suspend fun getRemoteKeys(): List<UserCryptoKeyRaw> {
        return triggerRemoteOnly { remote.getAllMyKeys() }
    }

    suspend fun clearAll() {
        local.clearAllKeys()
    }

    suspend fun getLocalRawKey(): UserCryptoKeyRaw? {
        return local.getMyKey()
    }

    suspend fun saveUserKeyLocally(keyRaw: UserCryptoKeyRaw, decryptedKey: Data) {
        local.saveKey(keyRaw, decryptedKey.asBase64String(cryptoCore.hashProvider))
    }

    suspend fun uploadNewUserKey(keyRaw: UserCryptoKeyRaw, removeOldKeys: Boolean = true): UserCryptoKeyRaw {
        return triggerRemoteOnly { remote.createPersonalKey(keyRaw, removeOldKeys) }
    }

    fun userKeyState(): Flow<Boolean> {
        return local.userKeyState()
    }
}