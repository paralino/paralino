package app.paralino.core.domain.crypto.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.crypto.UserAuthDerivedKeyV1
import app.paralino.core.models.raw.SchemeVersion
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

class CryptoAccountOperationV1(
    val cryptoCore: CryptoCore
) {

    suspend fun generateNewKeysForUser(
        authKey: UserAuthDerivedKeyV1,
        userId: String
    ): GeneratedUserKeys {
        val newSignatureKeyPair = cryptoCore.getNewSignatureKeyPair()
        return changeUserMasterKey(
            userId = userId,
            signatureKeyPair = newSignatureKeyPair,
            authKey = authKey
        )
    }

    suspend fun changeUserMasterKey(
        userId: String,
        signatureKeyPair: SimpleKeyPairData,
        authKey: UserAuthDerivedKeyV1,
    ): GeneratedUserKeys {
        val newPersonalKey = cryptoCore.getNewSymmetricKey()
        val dataKDF = cryptoCore.getDefaultDataKDFAlg()

        val masterKey = deriveUserMasterKey(
            userId = userId,
            masterAuthKey = authKey.masterAuthKey,
            alg = dataKDF
        )
        val encryptedPersonalKey = cryptoCore.encryptDataWithKey(
            data = newPersonalKey,
            key = masterKey
        )

        val encryptedPrivateIdentityKey = cryptoCore.encryptDataWithKey(
            data = signatureKeyPair.privateKey,
            key = newPersonalKey
        )
        val encryptedPublicIdentityKey = cryptoCore.encryptDataWithKey(
            data = signatureKeyPair.publicKey,
            key = newPersonalKey
        )

        val encryptedKeyData = UserCryptoKeyRaw(
            // id & version are not used in this case so they can be empty values
            "",
            userKeyVersion = 0,
            ownerUserId = userId,
            encryptedSymmetricKey = encryptedPersonalKey,
            encryptedPrivateKey = encryptedPrivateIdentityKey,
            encryptedPublicKey = encryptedPublicIdentityKey,
            publicKey = signatureKeyPair.publicKey,
            oldMasterKeys = null,
            loginSalt = authKey.loginSalt,
            dataSalt = null,
            schemeVersion = SchemeVersion.V1_0.key,
            loginAlg = BaseRemoteModule.formatter.encodeToString(authKey.kdfAlg),
            dataAlg = BaseRemoteModule.formatter.encodeToString(dataKDF),
        )

        val keys = GeneratedUserKeys(
            userPersonalKey = newPersonalKey,
            signatureKeyPair = signatureKeyPair,
            encryptedKeyData = encryptedKeyData
        )

        cryptoCore.verifyKeys(keys = keys, identityKeyPair = signatureKeyPair, key = masterKey)

        return keys
    }


    suspend fun encryptUserPersonalKey(
        userId: String,
        masterAuthKey: String,
        userPersonalKey: String,
        alg: KDFAlg
    ): String {
        val masterKey = deriveUserMasterKey(
            userId = userId,
            masterAuthKey = masterAuthKey,
            alg = alg
        )
        return cryptoCore.encryptDataWithKey(
            data = userPersonalKey,
            key = masterKey
        )
    }

    suspend fun decryptUserPersonalKey(
        masterAuthKey: String,
        userId: String,
        encryptedMasterKey: String,
        alg: KDFAlg
    ): String {
        val masterKey = deriveUserMasterKey(
            userId = userId,
            masterAuthKey = masterAuthKey,
            alg = alg
        )
        return cryptoCore.decryptDataWithKey(
            data = encryptedMasterKey,
            key = masterKey
        )
    }

    suspend fun deriveUserAuthKey(
        salt: String,
        password: String,
        alg: KDFAlg
    ): UserAuthDerivedKeyV1 {
        val masterAuthKey = cryptoCore.deriveKeyFromPassword(
            password = password,
            salt = salt,
            alg = alg
        )

        val loginKey = cryptoCore.deriveSymmetricKey(
            key = masterAuthKey,
            derivedKeyId = 0,
            context = "prlnauth"
        )

        return UserAuthDerivedKeyV1(
            masterAuthKey = masterAuthKey,
            loginKey = loginKey,
            loginSalt = salt,
            kdfAlg = alg
        )
    }

    private suspend fun deriveUserMasterKey(
        userId: String,
        masterAuthKey: String,
        alg: KDFAlg
    ): String {
        val masterSeedKey = cryptoCore.deriveKeyFromPassword(
            password = masterAuthKey,
            salt = userId,
            alg = alg
        )

        return cryptoCore.deriveSymmetricKey(
            key = masterSeedKey,
            derivedKeyId = 1,
            context = "prlndata"
        )
    }

    @Throws(IllegalStateException::class)
    suspend fun verifyUserKeysChange(
        oldKey: UserCryptoKeyRaw,
        oldMasterAuthKey: String,
        oldMasterKey: String,
    ) {
        check(
            decryptUserPersonalKey(
                masterAuthKey = oldMasterAuthKey,
                userId = oldKey.ownerUserId,
                encryptedMasterKey = oldKey.encryptedSymmetricKey,
                alg = BaseRemoteModule.formatter.decodeFromString(oldKey.dataAlg)
            ).contentEquals(oldMasterKey)
        ) { "Old key does not match" }
    }
}