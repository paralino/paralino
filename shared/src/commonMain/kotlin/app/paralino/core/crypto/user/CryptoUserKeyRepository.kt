package app.paralino.core.crypto.user

import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.flow.Flow

class CryptoUserKeyRepository(
    private val local: UserCryptoKeyLocalDataSource,
    private val remote: CryptoUserKeysRemoteSource,
) : BaseRepository() {

    suspend fun activePersonalKey(): String {
        return local.getCachedUserKey()
            .takeIf { it.isNotBlank() }
            ?: throw MissingUserKeyException(localCache = true)
    }

    suspend fun signatureKeyPair(): SimpleKeyPairData {
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

    suspend fun saveUserKeyLocally(keyRaw: UserCryptoKeyRaw, decryptedKey: String) {
        local.saveKey(keyRaw, decryptedKey)
    }

    suspend fun uploadNewUserKey(keyRaw: UserCryptoKeyRaw, removeOldKeys: Boolean = true): UserCryptoKeyRaw {
        return triggerRemoteOnly { remote.createPersonalKey(keyRaw, removeOldKeys) }
    }

    fun userKeyState(): Flow<Boolean> {
        return local.userKeyState()
    }
}