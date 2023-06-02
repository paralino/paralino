package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.data.db.UserKeysEntity
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.models.user.SessionCryptoKeyData

class UserKeyMapperCore constructor(
    private val cryptoCore: CryptoCore,
) {

    suspend fun mapKeyPair(model: UserCryptoKeyRaw, userKey: String): AsymmetricCryptoKeyData {
        val privateKey = cryptoCore.decryptDataWithKey(
            data = model.encryptedPrivateKey,
            key = userKey
        )

        val publicKey = cryptoCore.decryptDataWithKey(
            data = model.encryptedPublicKey,
            key = userKey
        )

        return AsymmetricCryptoKeyData(
            privateKey = privateKey,
            publicKey = publicKey,
            version = model.version
        )
    }

    fun remote2Local(model: UserCryptoKeyRaw, isActive: Boolean): UserKeysEntity {
        return UserKeysEntity(
            id = model.id,
            version = model.version,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            encryptedPublicKey = model.encryptedPublicKey,
            isActive = isActive
        )
    }

    fun local2Remote(model: UserKeysEntity, userId: String): UserCryptoKeyRaw {
        return UserCryptoKeyRaw(
            id = model.id,
            version = model.version ?: 0,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            encryptedPublicKey = model.encryptedPublicKey,
            ownerUserId = userId
        )
    }

    suspend fun wrapMasterKey(userMasterKey: String): SessionCryptoKeyData {
        val newSessionKey = cryptoCore.getNewSymmetricKey()
        val encryptedKey = cryptoCore.encryptDataWithKey(
            data = userMasterKey,
            key = newSessionKey
        )
        return SessionCryptoKeyData(encryptedKey = encryptedKey, sessionKey = newSessionKey)
    }

    suspend fun unwrapMasterKey(sessionMasterKey: String, encryptedUserMasterKey: String): String {
        return cryptoCore.decryptDataWithKey(
            data = encryptedUserMasterKey,
            key = sessionMasterKey
        )
    }

}