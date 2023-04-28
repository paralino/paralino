package app.paralino.core.crypto

interface ICryptoServiceProvider: ICryptoHashProvider {
    fun createNewSymmetricKey(): String
    fun createNewCryptoKeyPair(): KeyPair
    fun createNewSignatureKeyPair(): KeyPair

    fun encryptDataWithSymmetricKey(data: String, key: String): String
    fun decryptDataWithSymmetricKey(data: String, key: String): String

    fun encryptDataWithPassword(data: String, password: String, context: String): String
    fun decryptDataWithPassword(data: String, password: String, context: String): String

    fun encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String
    fun decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String

    fun signData(data: String, myPrivateKey: String): String
    fun verifySignature(data: String, publicKey: String): String?

}

interface ICryptoHashProvider {
    fun computeHash(data: String, type: HashType): String
}

enum class HashType {
    SHA256,
    SHA512
}
