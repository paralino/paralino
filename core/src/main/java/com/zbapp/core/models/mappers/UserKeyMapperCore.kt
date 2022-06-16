package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData

class UserKeyMapperCore constructor(
    private val cryptoCore: CryptoCore,
) {

    suspend fun mapKeyPair(model: UserCryptoKeyRaw, userKey: String): AsymmetricCryptoKeyData {
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

}