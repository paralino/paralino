package com.zbapp.core.crypto.user

import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData

interface ICryptoUserKeyLocalDataSource {

    fun getCachedUserKey(): String

    suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String)

    suspend fun getKeyPair(): AsymmetricCryptoKeyData?
    suspend fun getMyKey(): UserCryptoKeyRaw?

    suspend fun clearAllKeys()
    suspend fun deleteKey(id: String)

}