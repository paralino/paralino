package app.paralino.core.crypto

interface ICryptoServiceProvider : ICryptoHashProvider {

    fun createRandomId(length: Int, alphabet: AlphabetType): String

    fun createNewSymmetricKey(): String
    fun createNewCryptoKeyPair(): KeyPair
    fun createNewSignatureKeyPair(): KeyPair

    fun encryptDataWithSymmetricKey(data: String, key: String): String
    fun decryptDataWithSymmetricKey(data: String, key: String): String

    fun encryptDataWithPassword(
        data: String,
        password: String,
        context: String,
        useFastMode: Boolean
    ): String

    fun decryptDataWithPassword(
        data: String,
        password: String,
        context: String,
        useFastMode: Boolean
    ): String

    fun encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String
    fun decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String

    fun signData(data: String, myPrivateKey: String): String
    fun verifySignature(data: String, publicKey: String): String?
}

interface ICryptoHashProvider {
    fun computeHash(data: String, type: HashType): String
}

enum class AlphabetType(val alphabet: String) {
    SAFE(alphabet = "3456789ABCDEFGHJKLMNPQRTUWYZ"),
    EXTENDED(alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
}

enum class HashType {
    SHA256,
    SHA512
}

object PasswordHashingDefaults {
    fun getHashingIterations(useFastMode: Boolean, cryptoScheme: Int): Long {
        return when {
            cryptoScheme <= 1 -> 2
            else -> if (useFastMode) 2 else 5
        }
    }

    fun getHashingMemoryInBytes(useFastMode: Boolean, cryptoScheme: Int): Long {
        return when {
            cryptoScheme <= 1 -> 67108864
            // 64 or 256 MB
            else -> if (useFastMode) 67108864 else 268435456
        }
    }
}