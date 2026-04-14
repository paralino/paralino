package app.paralino.core.crypto

import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.utils.BaseRemoteModule

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

    fun getNewSymmetricKeyForLocationData(): String {
        return serviceProvider.createNewSymmetricKeyForDerivation()
    }

    fun getNewCryptoKeyPair(): SimpleKeyPairData {
        return serviceProvider.createNewCryptoKeyPair()
    }

    fun getNewSignatureKeyPair(): SimpleKeyPairData {
        return serviceProvider.createNewSignatureKeyPair()
    }

    suspend fun deriveKeyFromPassword(password: String, salt: String, alg: KDFAlg): String {
        return serviceProvider.deriveKeyFromPassword(password = password, salt = salt, alg = alg)
    }

    fun deriveSymmetricKey(key: String, derivedKeyId: Long, context: String): String {
        return serviceProvider.deriveSymmetricKey(key = key, derivedKeyId = derivedKeyId, context = context)
    }

    suspend fun verifyKeys(keys: GeneratedUserKeys, identityKeyPair: SimpleKeyPairData, key: String) {
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

    fun getPlaceEventKey(
        seed: String,
        isEnter: Boolean
    ): String {
        return hashProvider.computeHash(
            data = "${seed}_${if (isEnter) "enter" else "exit"}",
            type = HashType.GENERIC_256,
            encodingType = EncodingType.BASE64_URL
        )
    }
}

class GeneratedUserKeys(
    val userPersonalKey: String,
    val signatureKeyPair: SimpleKeyPairData,
    val encryptedKeyData: UserCryptoKeyRaw,
)