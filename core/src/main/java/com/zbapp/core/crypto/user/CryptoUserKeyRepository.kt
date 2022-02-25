package com.zbapp.core.crypto.user

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.Data
import com.zbapp.core.crypto.base64ToData
import com.zbapp.core.models.raw.AsymmetricKeyDataRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.utils.exceptions.MissingUserKeyException

class CryptoUserKeyRepository(
    private val cryptoCore: CryptoCore,
    private val local: ICryptoUserKeyLocalDataSource,
) {

    var personalKey: Data
        get() {
            return personalKeyOrNull ?: throw MissingUserKeyException(keyPair = false)
        }
        set(value) {
            personalKeyOrNull = value
        }

    var personalKeyOrNull: Data?
        get() {
            return local.getUserKey().takeIf { it.isNotBlank() }
                ?.base64ToData(cryptoCore.hashProvider)
                ?.takeIf { it.content.isNotEmpty() }
        }
        set(value) {
            local.saveUserKey(value?.asBase64String(cryptoCore.hashProvider) ?: "")
        }

    fun clearUserPersonalKey() {
        personalKeyOrNull = null
    }

    suspend fun saveKeyPairs(data: List<AsymmetricKeyDataRaw>) {
        local.recreateAllKeyPairs(data)
    }

    suspend fun saveKeyPair(key: AsymmetricKeyDataRaw) {
        local.saveKeyPair(key)
    }

    suspend fun latestKeyPair(): AsymmetricCryptoKeyData {
        return latestKeyPairOrNull() ?: throw MissingUserKeyException(keyPair = true)
    }

    suspend fun latestKeyPairOrNull(): AsymmetricCryptoKeyData? {
        return local.getLatestKeyPair()
    }

}