package app.paralino.core.crypto

interface ICryptoServiceProvider {
    fun encryptDataWithSymmetricKey(data: ByteArray, key: ByteArray, context: ByteArray?): Data
    fun decryptDataWithSymmetricKey(data: ByteArray, key: ByteArray, context: ByteArray?): Data

    fun encryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): Data
    fun decryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): Data

    fun encryptDataWithKeyPair(data: ByteArray, myPrivateKey: ByteArray, publicKey: ByteArray): Data
    fun decryptDataWithKeyPair(data: ByteArray, myPrivateKey: ByteArray, publicKey: ByteArray): Data

    fun signData(data: ByteArray, myPrivateKey: ByteArray): Data
    fun verifySignature(data: ByteArray, publicKey: ByteArray): Data?
}

interface ICryptoKeyProvider {
    fun createNewSymmetricKey(): Data
    fun createNewAsymmetricKey(): KeyPair
}

interface ICryptoHashProvider {
    fun toBase64(data: ByteArray): String
    fun fromBase64(data: String): Data
    fun toByteArray(data: String): Data
    fun byteArrayToString(data: ByteArray): String

    fun computeHash(data: ByteArray, type: HashType): Data
}

enum class HashType {
    SHA256,
    SHA512
}
