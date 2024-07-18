package app.paralino.core.crypto.user

import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.flow.Flow

class CryptoUserKeyRepository(
    private val local: UserCryptoKeyLocalDataSource,
    private val remote: CryptoUserKeysRemoteSource,
    private val userKeyMapper: UserKeyMapperCore
) : BaseRepository() {

    suspend fun activePersonalKey(): String {
        return activePersonalKeyWithVersion().key
    }

    suspend fun activePersonalKeyWithVersion(): SymmetricCryptoKeyData {
        return local.getCachedUserKey()
            .takeIf { it != null && it.key.isNotBlank() }
            ?: throw MissingUserKeyException(localCache = true)
    }

    suspend fun personalKey(version: Long): SymmetricCryptoKeyData {
        val rawKey = getLocalRawKey() ?: throw MissingUserKeyException(localCache = false)

        if (version == rawKey.userKeyVersion || version == 0L) {
            return activePersonalKeyWithVersion()
        }

        val oldKeys = rawKey.oldMasterKeys?.let {
            userKeyMapper.remote2Core(oldMasterKeys = it, userKey = activePersonalKey())
        } ?: throw MissingUserKeyException(localCache = false)

        val key = oldKeys.keys.firstOrNull {
            it.version == version
        } ?: throw MissingUserKeyException(localCache = false)

        return key
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