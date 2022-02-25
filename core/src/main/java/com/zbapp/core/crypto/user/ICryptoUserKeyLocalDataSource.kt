package com.zbapp.core.crypto.user

import com.zbapp.core.models.raw.AsymmetricKeyDataRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData

interface ICryptoUserKeyLocalDataSource {

    fun getUserKey(): String
    fun saveUserKey(key: String)
    fun clearUserKey()

    suspend fun recreateAllKeyPairs(keys: List<AsymmetricKeyDataRaw>)
    suspend fun saveKeyPair(key: AsymmetricKeyDataRaw)
    suspend fun getAllKeyPairs(): List<AsymmetricCryptoKeyData>
    suspend fun getLatestKeyPair(): AsymmetricCryptoKeyData?

    suspend fun clearAllKeys()

}