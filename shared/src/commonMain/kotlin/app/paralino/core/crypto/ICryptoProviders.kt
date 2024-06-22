package app.paralino.core.crypto

interface ICryptoServiceProvider : ICryptoHashProvider {

    fun createRandomId(length: Int, alphabet: AlphabetType): String

    fun createNewSymmetricKey(): String
    fun createNewCryptoKeyPair(): KeyPair
    fun createNewSignatureKeyPair(): KeyPair

    suspend fun deriveKeyFromPassword(password: String, salt: String, alg: KDFAlg): String

    /**
     * @param derivedKeyId Index or specific id that belongs to derived key.
     * @param context The context for derived key. Exactly 8 bytes.
     */
    fun deriveLongSymmetricKey(key: String, derivedKeyId: Long, context: String): String

    fun encryptDataWithSymmetricKey(data: String, key: String): String
    fun decryptDataWithSymmetricKey(data: String, key: String): String

    fun encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String
    fun decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String

    fun signData(data: String, myPrivateKey: String): String
    fun verifySignature(data: String, publicKey: String): String?

    fun changeEncoding(input: String, source: EncodingType, target: EncodingType): String
}

interface ICryptoHashProvider {
    fun computeHash(
        data: String,
        type: HashType,
        encodingType: EncodingType = EncodingType.HEX
    ): String
}

object PasswordHashingDefaults {
    fun getHashingIterations(mode: PasswordHashingMode, cryptoScheme: Int): Long {
        return when {
            cryptoScheme <= 1 -> 2
            else -> when(mode) {
                PasswordHashingMode.FAST -> 2
                PasswordHashingMode.NORMAL -> 3
                PasswordHashingMode.SLOW -> 4
            }
        }
    }

    fun getHashingMemoryInMB(mode: PasswordHashingMode, cryptoScheme: Int): Long {
        val mb = when {
            cryptoScheme <= 1 -> 64L
            // 64 or 256 MB
            else -> when(mode) {
                PasswordHashingMode.FAST -> 32L
                PasswordHashingMode.NORMAL -> 64L
                PasswordHashingMode.SLOW -> 128L
            }
        }

        return mb
    }
}