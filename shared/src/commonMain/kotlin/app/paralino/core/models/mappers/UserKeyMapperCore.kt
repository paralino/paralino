package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import app.paralino.core.db.main.keys.UserKeysEntity
import app.paralino.core.logg
import app.paralino.core.models.raw.PersonalLocationHistoryCryptoKeyRaw
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.OldMasterKeys
import app.paralino.core.models.user.SessionCryptoKeyData
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.utils.BaseRemoteModule

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
            userKeyVersion = model.userKeyVersion,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            encryptedPublicKey = model.encryptedPublicKey,
            publicKey = model.publicKey,
            oldMasterKeys = model.oldMasterKeys,
            loginSalt = model.loginSalt,
            loginAlg = model.loginAlg,
            dataAlg = model.dataAlg,
            dataSalt = model.dataSalt,
            schemeVersion = model.schemeVersion,
            isActive = isActive
        )
    }

    fun local2Remote(model: UserKeysEntity, userId: String): UserCryptoKeyRaw {
        return UserCryptoKeyRaw(
            id = model.id,
            userKeyVersion = model.userKeyVersion ?: 0,
            encryptedSymmetricKey = model.encryptedSymmetricKey,
            encryptedPrivateKey = model.encryptedPrivateKey,
            encryptedPublicKey = model.encryptedPublicKey,
            publicKey = model.publicKey,
            oldMasterKeys = model.oldMasterKeys,
            loginSalt = model.loginSalt,
            loginAlg = model.loginAlg,
            dataAlg = model.dataAlg,
            dataSalt = model.dataSalt,
            schemeVersion = model.schemeVersion,
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

    suspend fun unwrapMasterKey(
        sessionMasterKey: String,
        encryptedUserMasterKey: String,
        keyVersion: Long
    ): SymmetricCryptoKeyData {
        return SymmetricCryptoKeyData(
            key = cryptoCore.decryptDataWithKey(
                data = encryptedUserMasterKey,
                key = sessionMasterKey
            ),
            version = keyVersion
        )
    }

    suspend fun remote2Core(oldMasterKeys: String, userKey: String): OldMasterKeys {
        return cryptoCore.decryptDataWithKey(
            data = oldMasterKeys,
            key = userKey
        ).let {
            BaseRemoteModule.formatter.decodeFromString<OldMasterKeys>(it)
        }
    }

    fun local2Remote(model: PersonalHistoryKeyEntity): PersonalLocationHistoryCryptoKeyRaw {
        return PersonalLocationHistoryCryptoKeyRaw(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedHistoryKey = model.encryptedHistoryKey,
            userKeyVersion = model.userKeyVersion,
            historyKeyVersion = model.historyKeyVersion
        )
    }

    fun remote2Local(model: PersonalLocationHistoryCryptoKeyRaw): PersonalHistoryKeyEntity {
        return PersonalHistoryKeyEntity(
            id = model.id,
            ownerUserId = model.ownerUserId,
            encryptedHistoryKey = model.encryptedHistoryKey,
            userKeyVersion = model.userKeyVersion,
            historyKeyVersion = model.historyKeyVersion
        )
    }
}