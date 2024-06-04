package app.paralino.android.crpyto

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KeyPair
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.GenericHash
import com.goterl.lazysodium.interfaces.PwHash
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.Key
import io.viascom.nanoid.NanoId
import java.nio.charset.StandardCharsets

class CryptoFrameworkLibsodium : ICryptoServiceProvider {

    private val lazySodium = LazySodiumAndroid(
        SodiumAndroid(),
        StandardCharsets.UTF_8,
        HexMessageEncoder()
    )

    override fun createRandomId(length: Int, alphabet: AlphabetType): String {
        return NanoId.generate(
            size = length,
            alphabet = alphabet.alphabet
        )
    }

    override fun createNewSymmetricKey(): String {
        return lazySodium.cryptoSecretBoxKeygen().encode()
    }

    override fun createNewCryptoKeyPair(): KeyPair {
        val keyPair = lazySodium.cryptoBoxKeypair()
        return KeyPair(
            privateKey = keyPair.secretKey.encode(),
            publicKey = keyPair.publicKey.encode()
        )
    }

    override fun createNewSignatureKeyPair(): KeyPair {
        val keyPair = lazySodium.cryptoSignKeypair()
        return KeyPair(
            privateKey = keyPair.secretKey.encode(),
            publicKey = keyPair.publicKey.encode()
        )
    }

    override fun encryptDataWithSymmetricKey(data: String, key: String): String {
        val nonce = lazySodium.nonce(SecretBox.XSALSA20POLY1305_NONCEBYTES)
        return lazySodium.cryptoSecretBoxEasy(
            data,
            nonce,
            key.decodeKey()
        ).let { nonce.plus(it.decode()).encode() }
    }

    override fun decryptDataWithSymmetricKey(data: String, key: String): String {

        val source = data.decode()
        val nonce = source.sliceArray(0 until SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val content = source.sliceArray(SecretBox.XSALSA20POLY1305_NONCEBYTES until source.size)

        return lazySodium.cryptoSecretBoxOpenEasy(
            content.encode(),
            nonce,
            key.decodeKey()
        )
    }

    override fun encryptDataWithPassword(
        data: String,
        password: String,
        context: String
    ): String {

        val out = lazySodium.randomBytesBuf(GenericHash.BYTES_MIN)
        val input = lazySodium.bytes(context)
        if (!lazySodium.cryptoGenericHash(
                out,
                GenericHash.BYTES_MIN,
                input,
                input.size.toLong()
            )
        ) {
            throw SodiumException("Error creating password context hash")
        }

        val key = lazySodium.cryptoPwHash(
            password,
            SecretBox.KEYBYTES,
            out,
            PwHash.OPSLIMIT_INTERACTIVE,
            PwHash.MEMLIMIT_INTERACTIVE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )

        return encryptDataWithSymmetricKey(data, key)
    }

    override fun decryptDataWithPassword(
        data: String,
        password: String,
        context: String
    ): String {

        val out = lazySodium.randomBytesBuf(GenericHash.BYTES_MIN)
        val input = lazySodium.bytes(context)
        if (!lazySodium.cryptoGenericHash(
                out,
                GenericHash.BYTES_MIN,
                input,
                input.size.toLong()
            )
        ) {
            throw SodiumException("Error creating password context hash")
        }

        val key = lazySodium.cryptoPwHash(
            password,
            SecretBox.KEYBYTES,
            out,
            PwHash.OPSLIMIT_INTERACTIVE,
            PwHash.MEMLIMIT_INTERACTIVE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )

        return decryptDataWithSymmetricKey(data, key)
    }

    override fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {
        val nonce = lazySodium.nonce(SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val keyPair = com.goterl.lazysodium.utils.KeyPair(
            publicKey.decodeKey(),
            myPrivateKey.decodeKey()
        )

        return lazySodium.cryptoBoxEasy(
            data,
            nonce,
            keyPair
        ).let { nonce.plus(it.decode()).encode() }
    }

    override fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {

        val source = data.decode()
        val nonce = source.sliceArray(0 until SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val content = source.sliceArray(SecretBox.XSALSA20POLY1305_NONCEBYTES until source.size)

        val keyPair = com.goterl.lazysodium.utils.KeyPair(
            publicKey.decodeKey(),
            myPrivateKey.decodeKey()
        )

        return lazySodium.cryptoBoxOpenEasy(
            content.encode(),
            nonce,
            keyPair
        )
    }

    override fun signData(data: String, myPrivateKey: String): String {
        return lazySodium.cryptoSign(
            data,
            myPrivateKey
        )
    }

    override fun verifySignature(data: String, publicKey: String): String? {
        return lazySodium.cryptoSignOpen(
            data,
            publicKey.decodeKey()
        )
    }

    override fun computeHash(data: String, type: HashType): String {
        val hash = when (type) {
            HashType.SHA256 -> lazySodium.cryptoHashSha256(data)
            HashType.SHA512 -> lazySodium.cryptoHashSha512(data)
        }

        // for hash we always use hex
        return lazySodium.toHexStr(hash.decode())
    }

    private fun Key.encode(): String {
        return this.asBytes.encode()
    }

    private fun ByteArray.encode(): String {
        return lazySodium.encodeToString(this)
    }

    private fun String.decode(): ByteArray {
        return lazySodium.decodeFromString(this)
    }

    private fun String.decodeKey(): Key {
        return Key.fromBytes(this.decode())
    }
}