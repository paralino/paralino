package app.paralino.core.domain.crypto.user

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.crypto.UserAuthDerivedKeyV2
import app.paralino.core.models.raw.SchemeVersion
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

class CryptoAccountOperationV2(
    private val cryptoCore: CryptoCore,
) {

    suspend fun deriveUserAuthKey(
        email: String,
        password: String,
        alg: KDFAlg
    ): UserAuthDerivedKeyV2 {
        val masterAuthSeed = cryptoCore.deriveKeyFromPassword(
            password = password,
            salt = email,
            alg = alg
        )

        val masterAuthKey = cryptoCore.deriveKeyFromPassword(
            password = masterAuthSeed,
            salt = password,
            alg = alg
        )

        val loginKey = cryptoCore.deriveSymmetricKey(
            key = masterAuthKey,
            derivedKeyId = 0,
            context = "prlnauth"
        )

        return UserAuthDerivedKeyV2(
            loginKey = loginKey,
            kdfAlg = alg
        )
    }

    suspend fun generateNewKeysForUser(
        loginAlg: KDFAlg,
        password: String,
        userId: String
    ): GeneratedUserKeys {
        val newSignatureKeyPair = cryptoCore.getNewSignatureKeyPair()
        return changeUserMasterKey(
            userId = userId,
            password = password,
            signatureKeyPair = newSignatureKeyPair,
            loginAlg = loginAlg
        )
    }

    suspend fun changeUserMasterKey(
        userId: String,
        password: String,
        signatureKeyPair: SimpleKeyPairData,
        loginAlg: KDFAlg,
    ): GeneratedUserKeys {
        val newPersonalKey = cryptoCore.getNewSymmetricKey()
        val dataKDF = cryptoCore.getDefaultDataKDFAlg()
        val dataSalt = cryptoCore.randomId(
            length = 32,
            alphabet = AlphabetType.EXTENDED
        )

        val masterKey = deriveUserMasterKey(
            password = password,
            userId = userId,
            dataSalt = dataSalt,
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
            loginSalt = cryptoCore.randomId(
                length = 32,
                alphabet = AlphabetType.EXTENDED
            ),
            dataSalt = dataSalt,
            schemeVersion = SchemeVersion.V2_0.key,
            loginAlg = BaseRemoteModule.formatter.encodeToString(loginAlg),
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

    private suspend fun deriveUserMasterKey(
        password: String,
        userId: String,
        dataSalt: String,
        alg: KDFAlg
    ): String {
        val masterSeedKey = cryptoCore.deriveKeyFromPassword(
            password = password,
            salt = cryptoCore.hashProvider.computeHash(
                data = userId + dataSalt,
                type = HashType.GENERIC_256,
                encodingType = EncodingType.HEX
            ),
            alg = alg
        )

        return cryptoCore.deriveSymmetricKey(
            key = masterSeedKey,
            derivedKeyId = 1,
            context = "prlndata"
        )
    }

    suspend fun decryptUserPersonalKey(
        password: String,
        userId: String,
        dataSalt: String,
        encryptedMasterKey: String,
        alg: KDFAlg
    ): String {
        val masterKey = deriveUserMasterKey(
            password = password,
            userId = userId,
            dataSalt = dataSalt,
            alg = alg
        )
        return cryptoCore.decryptDataWithKey(
            data = encryptedMasterKey,
            key = masterKey
        )
    }

    @Throws(IllegalStateException::class)
    suspend fun verifyUserKeysChange(
        oldKey: UserCryptoKeyRaw,
        oldPassword: String,
        oldMasterKey: String,
    ) {
        check(
            decryptUserPersonalKey(
                password = oldPassword,
                userId = oldKey.ownerUserId,
                dataSalt = oldKey.dataSalt!!,
                encryptedMasterKey = oldKey.encryptedSymmetricKey,
                alg = BaseRemoteModule.formatter.decodeFromString(oldKey.dataAlg)
            ).contentEquals(oldMasterKey)
        ) { "Old key does not match" }
    }

    @Throws(IllegalStateException::class)
    suspend fun verifyUserKeysChange(
        oldKey: UserCryptoKeyRaw,
        newKey: UserCryptoKeyRaw,
        newPassword: String,
        oldMasterKey: String,
        newMasterKey: String
    ) {
        check(
            decryptUserPersonalKey(
                password = newPassword,
                userId = newKey.ownerUserId,
                dataSalt = newKey.dataSalt!!,
                encryptedMasterKey = newKey.encryptedSymmetricKey,
                alg = BaseRemoteModule.formatter.decodeFromString(newKey.dataAlg)
            ).contentEquals(newMasterKey)
        ) { "New key does not match" }

        check(
            cryptoCore.decryptDataWithKey(
                data = oldKey.encryptedPrivateKey,
                key = oldMasterKey
            ).contentEquals(
                cryptoCore.decryptDataWithKey(
                    data = newKey.encryptedPrivateKey,
                    key = newMasterKey
                )
            )
        ) { "Private key does not match" }

        check(
            cryptoCore.decryptDataWithKey(
                data = oldKey.encryptedPublicKey,
                key = oldMasterKey
            ).contentEquals(
                cryptoCore.decryptDataWithKey(
                    data = newKey.encryptedPublicKey,
                    key = newMasterKey
                )
            )
        ) { "Public key does not match" }

        check(oldKey.publicKey == newKey.publicKey) {
            "Public key does not match"
        }
    }
}