package app.paralino.core.crypto.user

import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import kotlinx.coroutines.flow.Flow

interface ICryptoUserKeyLocalDataSource {

    fun getCachedUserKey(): String

    suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String)

    suspend fun getKeyPair(): AsymmetricCryptoKeyData?
    suspend fun getMyKey(): UserCryptoKeyRaw?
    fun userKeyState(): Flow<Boolean>

    suspend fun clearAllKeys()
    suspend fun deleteKey(id: String)

}