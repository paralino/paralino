package app.paralino.core.crypto.testutils

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.SimpleKeyPairData

/**
 * Deterministic, non-cryptographic provider for shared tests.
 *
 * It exists to make shared logic testable in `commonTest` without depending on platform crypto libs.
 */
class FakeCryptoServiceProvider(
    private var counter: Int = 0,
) : ICryptoServiceProvider {

    private fun nextId(prefix: String): String = "${prefix}_${counter++}"

    override fun createRandomId(length: Int, alphabet: AlphabetType): String {
        // We don't need true randomness for tests; keep it deterministic.
        return nextId("rid").take(length).padEnd(length, '0')
    }

    override fun createNewSymmetricKey(): String = nextId("sym")

    override fun createNewSymmetricKeyForDerivation(): String = nextId("symd")

    override fun createNewCryptoKeyPair(): SimpleKeyPairData {
        val id = nextId("boxkp")
        return SimpleKeyPairData(privateKey = "${id}_priv", publicKey = "${id}_pub")
    }

    override fun createNewSignatureKeyPair(): SimpleKeyPairData {
        val id = nextId("sigkp")
        return SimpleKeyPairData(privateKey = "${id}_priv", publicKey = "${id}_pub")
    }

    override suspend fun deriveKeyFromPassword(password: String, salt: String, alg: KDFAlg): String {
        return "kdf(${password}|${salt}|${alg.kdf}|${alg.rounds}|${alg.memoryMb}|${alg.keyBytes}|${alg.encoding})"
    }

    override fun deriveSymmetricKey(key: String, derivedKeyId: Long, context: String): String {
        return "derive(${key}|${derivedKeyId}|${context})"
    }

    override fun encryptDataWithSymmetricKey(data: String, key: String): String {
        // Toy “encryption”: bind ciphertext to key; easy to validate in decrypt.
        return "enc(${key})::${data}"
    }

    override fun decryptDataWithSymmetricKey(data: String, key: String): String {
        val prefix = "enc(${key})::"
        check(data.startsWith(prefix)) { "Wrong key" }
        return data.removePrefix(prefix)
    }

    override fun encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String {
        return "box(${myPrivateKey}|${publicKey})::${data}"
    }

    override fun decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String {
        val prefix = "box(${myPrivateKey}|${publicKey})::"
        check(data.startsWith(prefix)) { "Wrong keypair" }
        return data.removePrefix(prefix)
    }

    override fun signData(data: String, myPrivateKey: String): String {
        return "sig(${myPrivateKey})::${data}"
    }

    override fun verifySignature(data: String, publicKey: String): String? {
        // This fake does not model asymmetric verification; keep it predictable:
        // if the payload looks signed, return the original message; otherwise null.
        return data.substringAfter("::", missingDelimiterValue = "").takeIf { it.isNotEmpty() }
    }

    override fun computeHash(data: String, type: HashType, encodingType: EncodingType): String {
        return "hash(${type.bytes}|${encodingType.key})::${data}"
    }
}

