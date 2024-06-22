package app.paralino.android.crpyto

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.KDFType
import app.paralino.core.crypto.KeyPair
import app.paralino.core.utils.exceptions.UnsupportedEncryptionSchemeException
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.KeyDerivation
import com.goterl.lazysodium.interfaces.MessageEncoder
import com.goterl.lazysodium.interfaces.PwHash
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.Key
import com.sun.jna.NativeLong
import io.viascom.nanoid.NanoId
import java.nio.charset.StandardCharsets
import java.util.Base64

class CryptoFrameworkLibsodium(private val cryptoScheme: Int) : ICryptoServiceProvider {

    private val hexEncoder = HexMessageEncoder()
    private val base64Encoder = object : MessageEncoder {
        override fun encode(cipher: ByteArray): String {
            return Base64.getEncoder().encodeToString(cipher)
        }

        override fun decode(cipherText: String): ByteArray {
            return Base64.getDecoder().decode(cipherText)
        }
    }
    private val base64UrlEncoder = object : MessageEncoder {
        override fun encode(cipher: ByteArray): String {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(cipher)
        }

        override fun decode(cipherText: String): ByteArray {
            return Base64.getUrlDecoder().decode(cipherText)
        }
    }

    private val sodiumAndroid = SodiumAndroid()
    private val lazySodiumHex = LazySodiumAndroid(
        sodiumAndroid,
        StandardCharsets.UTF_8,
        hexEncoder
    )
    private val lazySodiumBase64 = LazySodiumAndroid(
        sodiumAndroid,
        StandardCharsets.UTF_8,
        base64Encoder
    )
    private val lazySodiumBase64Url = LazySodiumAndroid(
        sodiumAndroid,
        StandardCharsets.UTF_8,
        base64UrlEncoder
    )

    private val lazySodium = lazySodiumBase64Url

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

    override fun deriveLongSymmetricKey(
        key: String,
        derivedKeyId: Long,
        context: String
    ): String {
        return lazySodium.cryptoKdfDeriveFromKey(
            KeyDerivation.BYTES_MAX,
            derivedKeyId,
            context,
            key.decodeKey()
        ).encode()
    }

    override suspend fun deriveKeyFromPassword(
        password: String,
        salt: String,
        alg: KDFAlg
    ): String {
        when (alg.kdfType) {
            KDFType.ARGON2ID -> {
                val saltOutput = lazySodium.randomBytesBuf(PwHash.ARGON2ID_SALTBYTES)
                val saltInput = lazySodium.bytes(salt)
                if (
                    !lazySodium.cryptoGenericHash(
                        saltOutput,
                        PwHash.ARGON2ID_SALTBYTES,
                        saltInput,
                        saltInput.size.toLong()
                    )
                ) {
                    throw SodiumException("Error creating password context hash")
                }

                val key = lazySodium.cryptoPwHash(
                    password,
                    alg.keyBytes,
                    saltOutput,
                    alg.rounds,
                    NativeLong(alg.memoryBytes),
                    PwHash.Alg.PWHASH_ALG_ARGON2ID13
                )

                return key
            }

            null -> throw UnsupportedEncryptionSchemeException(scheme = alg.kdf)
        }
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

    override fun computeHash(data: String, type: HashType, encodingType: EncodingType): String {

        val message: ByteArray = lazySodium.bytes(data)
        val hash: ByteArray = lazySodium.randomBytesBuf(type.bytes)
        val res: Boolean =
            lazySodium.cryptoGenericHash(hash, hash.size, message, message.size.toLong())

        if (!res) {
            throw SodiumException("Could not hash the message.")
        }

        return when (encodingType) {
            EncodingType.HEX -> hexEncoder.encode(hash)
            EncodingType.BASE64 -> base64Encoder.encode(hash)
            EncodingType.BASE64_URL -> base64UrlEncoder.encode(hash)
        }
    }

    override fun changeEncoding(input: String, source: EncodingType, target: EncodingType): String {

        val encoder: MessageEncoder
        val decoder: MessageEncoder

        when (source) {
            EncodingType.HEX -> {
                decoder = hexEncoder
                encoder = when (target) {
                    EncodingType.HEX -> return input
                    EncodingType.BASE64 -> base64Encoder
                    EncodingType.BASE64_URL -> base64UrlEncoder
                }
            }

            EncodingType.BASE64 -> {
                decoder = base64Encoder
                encoder = when (target) {
                    EncodingType.HEX -> hexEncoder
                    EncodingType.BASE64 -> return input
                    EncodingType.BASE64_URL -> base64UrlEncoder
                }
            }

            EncodingType.BASE64_URL -> {
                decoder = base64UrlEncoder
                encoder = when (target) {
                    EncodingType.HEX -> hexEncoder
                    EncodingType.BASE64 -> base64Encoder
                    EncodingType.BASE64_URL -> return input
                }
            }
        }

        return encoder.encode(decoder.decode(input))
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