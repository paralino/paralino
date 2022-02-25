package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.raw.AsymmetricKeyDataRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.locationtrack.data.models.user.UserKeyEntity

class UserKeyMapper(
    private val cryptoCore: CryptoCore
) {
    suspend fun mapKeyPair(model: UserKeyEntity, userKey: String): AsymmetricCryptoKeyData {
        val privateKey = cryptoCore.decryptDataWithKey(
            data = model.encryptedPrivateKey,
            key = userKey
        )
        return AsymmetricCryptoKeyData(
            privateKey = privateKey,
            publicKey = model.publicKey,
            version = model.version
        )
    }

    suspend fun mapKeyPair(model: AsymmetricCryptoKeyData, userKey: String): UserKeyEntity {
        val encryptedPrivateKey = cryptoCore.encryptDataWithKey(
            data = model.privateKey,
            key = userKey
        )
        return UserKeyEntity(
            id = 0,
            encryptedPrivateKey = encryptedPrivateKey,
            publicKey = model.publicKey,
            version = model.version
        )
    }

    suspend fun mapKeyPair(data: AsymmetricKeyDataRaw, userKey: String): UserKeyEntity {
        val model = cryptoCore.decryptModel<AsymmetricCryptoKeyData>(
            data = data,
            key = userKey
        )
        return mapKeyPair(model, userKey)
    }
}