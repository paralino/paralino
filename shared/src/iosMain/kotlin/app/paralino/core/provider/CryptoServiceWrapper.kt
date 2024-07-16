package app.paralino.core.provider

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.KeyPair
import app.paralino.core.logg
import app.paralino.core.models.Payload
import app.paralino.core.models.getOrThrow

class CryptoServiceWrapperIOS(
    private val cryptoIOS: ICryptoServiceProviderIOS
) : ICryptoServiceProvider {

    override fun createRandomId(length: Int, alphabet: AlphabetType): String {
        return cryptoIOS.createRandomId(length, alphabet).getOrThrow()
    }

    override fun createNewSymmetricKey(): String {
        return cryptoIOS.createNewSymmetricKey().getOrThrow()
    }

    override fun createNewSymmetricKeyForDerivation(): String {
        return cryptoIOS.createNewSymmetricKeyForDerivation().getOrThrow()
    }

    override fun createNewCryptoKeyPair(): KeyPair {
        return cryptoIOS.createNewCryptoKeyPair().getOrThrow()
    }

    override fun createNewSignatureKeyPair(): KeyPair {
        return cryptoIOS.createNewSignatureKeyPair().getOrThrow()
    }

    override suspend fun deriveKeyFromPassword(
        password: String,
        salt: String,
        alg: KDFAlg
    ): String {
        return cryptoIOS.deriveKeyFromPassword(password = password, salt = salt, alg = alg)
            .getOrThrow()
    }

    override fun deriveSymmetricKey(key: String, derivedKeyId: Long, context: String): String {
        return cryptoIOS.deriveSymmetricKey(key, derivedKeyId, context).getOrThrow()
    }

    override fun encryptDataWithSymmetricKey(data: String, key: String): String {
        return cryptoIOS.encryptDataWithSymmetricKey(data, key).getOrThrow()
    }

    override fun decryptDataWithSymmetricKey(data: String, key: String): String {
        return try {
            cryptoIOS.decryptDataWithSymmetricKey(data, key).getOrThrow()
        } catch (e: Exception) {
            logg.i("Error with decryptDataWithSymmetricKey: ${e.message}")
            throw e
        }
    }

    override fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {
        return cryptoIOS.encryptDataWithKeyPair(data, myPrivateKey, publicKey).getOrThrow()
    }

    override fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {
        return cryptoIOS.decryptDataWithKeyPair(data, myPrivateKey, publicKey).getOrThrow()
    }

    override fun signData(data: String, myPrivateKey: String): String {
        return cryptoIOS.signData(data, myPrivateKey).getOrThrow()
    }

    override fun verifySignature(data: String, publicKey: String): String? {
        return try {
            cryptoIOS.verifySignature(data, publicKey).getOrThrow()
        } catch (e: Exception) {
            return null
        }
    }

    override fun computeHash(data: String, type: HashType, encodingType: EncodingType): String {
        return cryptoIOS.computeHash(data, type, encodingType).getOrThrow()
    }
}

interface ICryptoServiceProviderIOS {
    fun createRandomId(length: Int, alphabet: AlphabetType): Payload<String>
    fun createNewSymmetricKey(): Payload<String>
    fun createNewSymmetricKeyForDerivation(): Payload<String>
    fun createNewCryptoKeyPair(): Payload<KeyPair>
    fun createNewSignatureKeyPair(): Payload<KeyPair>
    fun encryptDataWithSymmetricKey(data: String, key: String): Payload<String>
    fun decryptDataWithSymmetricKey(data: String, key: String): Payload<String>

    fun deriveSymmetricKey(key: String, derivedKeyId: Long, context: String): Payload<String>
    suspend fun deriveKeyFromPassword(password: String, salt: String, alg: KDFAlg): Payload<String>

    fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): Payload<String>

    fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): Payload<String>

    fun signData(data: String, myPrivateKey: String): Payload<String>
    fun verifySignature(data: String, publicKey: String): Payload<String>
    fun computeHash(data: String, type: HashType, encodingType: EncodingType): Payload<String>
}