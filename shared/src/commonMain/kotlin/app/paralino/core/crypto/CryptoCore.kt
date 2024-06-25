package app.paralino.core.crypto

import app.paralino.core.BuildKonfig
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

class CryptoCore(
    private val serviceProvider: ICryptoServiceProvider,
) {
    val hashProvider: ICryptoHashProvider = serviceProvider

    fun randomId(length: Int, alphabet: AlphabetType): String {
        return serviceProvider.createRandomId(length, alphabet)
    }

    fun getNewSymmetricKey(): String {
        return serviceProvider.createNewSymmetricKey()
    }

    fun getNewCryptoKeyPair(): KeyPair {
        return serviceProvider.createNewCryptoKeyPair()
    }

    suspend fun generateNewKeysForUser(
        authKey: UserAuthDerivedKey,
        userId: String
    ): GeneratedUserKeys {

        val newPersonalKey = getNewSymmetricKey()

        val dataKDF = getDefaultDataKDFAlg()
        val masterKey = deriveUserMasterKey(
            userId = userId,
            masterAuthKey = authKey.masterAuthKey,
            alg = dataKDF
        )
        val encryptedPersonalKey = encryptDataWithKey(
            data = newPersonalKey,
            key = masterKey
        )

        val newSignatureKeyPair = serviceProvider.createNewSignatureKeyPair()
        val encryptedPrivateIdentityKey = encryptDataWithKey(
            data = newSignatureKeyPair.privateKey,
            key = newPersonalKey
        )
        val encryptedPublicIdentityKey = encryptDataWithKey(
            data = newSignatureKeyPair.publicKey,
            key = newPersonalKey
        )

        val encryptedKeyData = UserCryptoKeyRaw(
            // id & version are not used in this case so they can be empty values
            "",
            version = 0,
            ownerUserId = userId,
            encryptedSymmetricKey = encryptedPersonalKey,
            encryptedPrivateKey = encryptedPrivateIdentityKey,
            encryptedPublicKey = encryptedPublicIdentityKey,
            loginSalt = authKey.loginSalt,
            loginAlg = BaseRemoteModule.formatter.encodeToString(authKey.kdfAlg),
            dataAlg = BaseRemoteModule.formatter.encodeToString(dataKDF),
        )

        val keys = GeneratedUserKeys(newPersonalKey, encryptedKeyData)

        verifyKeys(keys = keys, identityKeyPair = newSignatureKeyPair, key = masterKey)

        return keys
    }

    private suspend fun verifyKeys(keys: GeneratedUserKeys, identityKeyPair: KeyPair, key: String) {
        check(
            decryptDataWithKey(
                data = keys.encryptedKeyData.encryptedSymmetricKey,
                key = key
            ).contentEquals(keys.userPersonalKey)
        )
        check(
            decryptDataWithKey(
                data = keys.encryptedKeyData.encryptedPrivateKey,
                key = keys.userPersonalKey
            ).contentEquals(identityKeyPair.privateKey)
        )
        check(
            decryptDataWithKey(
                data = keys.encryptedKeyData.encryptedPublicKey,
                key = keys.userPersonalKey
            ).contentEquals(identityKeyPair.publicKey)
        )
    }

    suspend fun verifyUserKeysChange(
        oldKey: UserCryptoKeyRaw,
        newKey: UserCryptoKeyRaw,
        oldMasterAuthKey: String,
        newMasterAuthKey: String,
        userPersonalKey: String
    ) {
        check(
            decryptUserPersonalKey(
                masterAuthKey = oldMasterAuthKey,
                userId = oldKey.ownerUserId,
                encryptedMasterKey = oldKey.encryptedSymmetricKey,
                alg = BaseRemoteModule.formatter.decodeFromString(oldKey.dataAlg)
            ).contentEquals(userPersonalKey)
        ) { "Old key does not match" }

        check(
            decryptUserPersonalKey(
                masterAuthKey = newMasterAuthKey,
                userId = newKey.ownerUserId,
                encryptedMasterKey = newKey.encryptedSymmetricKey,
                alg = BaseRemoteModule.formatter.decodeFromString(newKey.dataAlg)
            ).contentEquals(userPersonalKey)
        ) { "New key does not match" }

        check(
            decryptDataWithKey(
                data = oldKey.encryptedPrivateKey,
                key = userPersonalKey
            ).contentEquals(
                decryptDataWithKey(
                    data = newKey.encryptedPrivateKey,
                    key = userPersonalKey
                )
            )
        ) { "Private key does not match" }

        check(
            decryptDataWithKey(
                data = oldKey.encryptedPublicKey,
                key = userPersonalKey
            ).contentEquals(
                decryptDataWithKey(
                    data = newKey.encryptedPublicKey,
                    key = userPersonalKey
                )
            )
        ) { "Public key does not match" }
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
        return encryptDataWithKey(
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
        return decryptDataWithKey(
            data = encryptedMasterKey,
            key = masterKey
        )
    }

    suspend inline fun <reified T> encryptModel(data: T, key: String): String {
        return encryptDataWithKey(BaseRemoteModule.formatter.encodeToString(data), key)
    }

    suspend fun encryptDataWithKey(data: String, key: String): String {
        return serviceProvider.encryptDataWithSymmetricKey(
            data = data,
            key = key
        )
    }

    suspend fun decryptDataWithKey(data: String, key: String): String {
        return serviceProvider.decryptDataWithSymmetricKey(
            data = data,
            key = key
        )
    }

    suspend fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String,
    ): String {
        return serviceProvider.encryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey,
            publicKey = publicKey
        )
    }

    suspend fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String,
    ): String {
        return serviceProvider.decryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey,
            publicKey = publicKey
        )
    }

    suspend fun decryptKeyWithKeyPair(
        key: String,
        myPrivateKey: String,
        publicKey: String
    ): String {
        return decryptDataWithKeyPair(
            data = key,
            myPrivateKey = myPrivateKey,
            publicKey = publicKey
        )
    }

    fun signData(data: String, myPrivateKey: String): String {
        return serviceProvider.signData(data, myPrivateKey)
    }

    fun verifySignature(data: String, publicKey: String): String? {
        return serviceProvider.verifySignature(data, publicKey)
    }

    suspend fun deriveParalinoIdAuthKey(code: String): String {
        val masterKey = serviceProvider.deriveKeyFromPassword(
            password = code,
            // using hardcoded, plain text salt here since code is the only input
            salt = "Fleur_de_sel",
            alg = getKDFAlg(mode = PasswordHashingMode.FAST)
        )

        return serviceProvider.computeHash(
            data = masterKey,
            type = HashType.GENERIC_256,
            encodingType = EncodingType.BASE64_URL
        )
    }

    suspend fun deriveParalinoIdMasterKeys(
        code: String,
        userId: String
    ): DerivedPasswords {

        val authKey = deriveParalinoIdAuthKey(code = code)
        val masterKey = serviceProvider.deriveKeyFromPassword(
            password = code,
            salt = userId,
            alg = getKDFAlg(mode = PasswordHashingMode.FAST)
        )

        return DerivedPasswords(
            loginPassword = authKey,
            backupPassword = masterKey
        )
    }

    suspend fun deriveUserAuthKey(
        salt: String,
        password: String,
        alg: KDFAlg
    ): UserAuthDerivedKey {
        val masterAuthKey = serviceProvider.deriveKeyFromPassword(
            password = password,
            salt = salt,
            alg = alg
        )

        val loginKey = serviceProvider.deriveSymmetricKey(
            key = masterAuthKey,
            derivedKeyId = 0,
            context = "prlnauth"
        )

        return UserAuthDerivedKey(
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
        val masterSeedKey = serviceProvider.deriveKeyFromPassword(
            password = masterAuthKey,
            salt = userId,
            alg = alg
        )

        return serviceProvider.deriveSymmetricKey(
            key = masterSeedKey,
            derivedKeyId = 1,
            context = "prlndata"
        )
    }

    fun getDefaultLoginKDFAlg(): KDFAlg {
        return getKDFAlg(mode = PasswordHashingMode.NORMAL)
    }

    fun getDefaultDataKDFAlg(): KDFAlg {
        return getKDFAlg(mode = PasswordHashingMode.SLOW)
    }

    private fun getKDFAlg(mode: PasswordHashingMode): KDFAlg {
        return KDFAlg(
            encoding = EncodingType.BASE64_URL.key,
            kdf = KDFType.ARGON2ID.key,
            memoryMb = when(mode) {
                PasswordHashingMode.FAST -> 32L
                PasswordHashingMode.NORMAL -> 64L
                PasswordHashingMode.SLOW -> 128L
            },
            rounds = when(mode) {
                PasswordHashingMode.FAST -> 2
                PasswordHashingMode.NORMAL -> 3
                PasswordHashingMode.SLOW -> 5
            },
            keyBytes = 32
        )
    }
}

class GeneratedUserKeys(
    val userPersonalKey: String,
    val encryptedKeyData: UserCryptoKeyRaw,
)