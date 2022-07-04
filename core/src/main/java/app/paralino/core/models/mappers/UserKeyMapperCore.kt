package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData

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