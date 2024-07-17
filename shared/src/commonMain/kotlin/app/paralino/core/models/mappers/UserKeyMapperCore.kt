package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.data.db.UserKeysEntity
import app.paralino.core.logg
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.SessionCryptoKeyData

class UserKeyMapperCore constructor(
    private val cryptoCore: CryptoCore,
) {

    suspend fun mapKeyPair(model: UserCryptoKeyRaw, userKey: String): SimpleKeyPairData {
        val privateKey = cryptoCore.decryptDataWithKey(
            data = model.encryptedPrivateKey,
            key = userKey
        )

        val publicKey = cryptoCore.decryptDataWithKey(
            data = model.encryptedPublicKey,
            key = userKey
        )

        check(publicKey == model.publicKey) {
            logg.i("User public key are not matching")
        }

        return SimpleKeyPairData(
            privateKey = privateKey,
            publicKey = publicKey
        )
    }

    fun remote2Local(model: UserCryptoKeyRaw, isActive: Boolean): UserKeysEntity {
        return UserKeysEntity(
            id = model.id,
            version = model.version,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            encryptedPublicKey = model.encryptedPublicKey,
            publicKey = model.publicKey,
            oldMasterKeys = model.oldMasterKeys,
            loginSalt = model.loginSalt,
            loginAlg = model.loginAlg,
            dataAlg = model.dataAlg,
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
            publicKey = model.publicKey,
            oldMasterKeys = model.oldMasterKeys,
            loginSalt = model.loginSalt,
            loginAlg = model.loginAlg,
            dataAlg = model.dataAlg,
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