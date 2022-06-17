package com.zbapp.core.crypto.user

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.Data
import com.zbapp.core.crypto.base64ToData
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.repo.base.BaseRepository
import com.zbapp.core.repo.user.CryptoUserKeysRemoteSource
import com.zbapp.core.utils.exceptions.MissingUserKeyException
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

    suspend fun uploadNewUserKey(keyRaw: UserCryptoKeyRaw): UserCryptoKeyRaw {
        return triggerRemoteOnly { remote.createPersonalKey(keyRaw) }
    }

    fun deleteKey(id: String): Flow<Unit> {
        return getDataOnce(
            remote = { remote.deleteUserKey(id) },
            localUpdate = { deleteLocalKey(id) }
        )
    }

    suspend fun deleteLocalKey(id: String) {
        local.deleteKey(id)
    }

}