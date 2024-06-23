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
}

interface ICryptoHashProvider {
    fun computeHash(
        data: String,
        type: HashType,
        encodingType: EncodingType = EncodingType.HEX
    ): String
}