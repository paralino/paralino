package app.paralino.android.crpyto

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CipherAlg
import app.paralino.core.crypto.CipherAlgType
import app.paralino.core.crypto.EncodingType
import app.paralino.core.crypto.HashType
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KDFAlg
import app.paralino.core.crypto.KDFType
import app.paralino.core.crypto.SimpleKeyPairData
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.exceptions.EncryptionException
import app.paralino.core.utils.exceptions.UnsupportedEncryptionSchemeException
import app.paralino.parasodium.AEAD
import app.paralino.parasodium.Box
import app.paralino.parasodium.HexMessageEncoder
import app.paralino.parasodium.KeyDerivation
import app.paralino.parasodium.MessageEncoder
import app.paralino.parasodium.ParasodiumAndroid
import app.paralino.parasodium.PwHash
import app.paralino.parasodium.Sign
import app.paralino.parasodium.models.Key
import io.viascom.nanoid.NanoId
import java.util.Base64

class CryptoFrameworkLibsodium : ICryptoServiceProvider {

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

    private val messageEncoder = base64UrlEncoder
    private val paraSodium = ParasodiumAndroid()

    override fun createRandomId(length: Int, alphabet: AlphabetType): String {
        return NanoId.generate(
            size = length,
            alphabet = alphabet.alphabet
        )
    }

    override fun createNewSymmetricKey(): String {
        val key = paraSodium.randomBytesBuf(AEAD.XCHACHA20POLY1305_IETF_KEYBYTES)
        paraSodium.cryptoAeadXChaCha20Poly1305IetfKeygen(key)
        return key.encodeWithCipherAlg()
    }

    override fun createNewSymmetricKeyForDerivation(): String {
        val key = paraSodium.randomBytesBuf(AEAD.XCHACHA20POLY1305_IETF_KEYBYTES)
        paraSodium.cryptoAeadXChaCha20Poly1305IetfKeygen(key)
        return key.encodeWithCipherAlg(algType = CipherAlgType.DEFAULT_KEY_DERIVE)
    }

    override fun createNewCryptoKeyPair(): SimpleKeyPairData {
        val keyPair = paraSodium.cryptoBoxKeypair()
        return SimpleKeyPairData(
            privateKey = keyPair.secretKey.getAsBytes().encodeWithCipherAlg(),
            publicKey = keyPair.publicKey.getAsBytes().encodeWithCipherAlg(),
        )
    }

    override fun createNewSignatureKeyPair(): SimpleKeyPairData {
        val keyPair = paraSodium.cryptoSignKeypair()
        return SimpleKeyPairData(
            privateKey = keyPair.secretKey.getAsBytes().encodeWithCipherAlg(),
            publicKey = keyPair.publicKey.getAsBytes().encodeWithCipherAlg()
        )
    }

    override fun encryptDataWithSymmetricKey(data: String, key: String): String {
        val decodedKey = key.decodeAsCipherPayload()
        when (decodedKey.algorithm) {
            CipherAlgType.DEFAULT -> {
                return encryptDataWithSymmetricKey(
                    data = data,
                    key = decodedKey.decodeData()
                ).encodeWithCipherAlg(
                    algType = CipherAlgType.DEFAULT,
                    payload = null
                )
            }

            CipherAlgType.DEFAULT_KEY_DERIVE -> {
                // we are using random key id, and not a counter or some internal state
                // since realtime locations don't have order, this way we make it hard to guess
                // how many times location was updated compared to using a counter
                // key id is not sensitive and is stored and delivered in plain text
                val keyId = paraSodium.randomBytesRandom()
                val newKey = deriveSymmetricKey(
                    key = decodedKey.data,
                    derivedKeyId = keyId,
                    context = "_derive_"
                )
                return encryptDataWithSymmetricKey(
                    data = data,
                    key = newKey.decodeAsCipherPayload().decodeData()
                )
                    .encodeWithCipherAlg(
                        algType = CipherAlgType.DEFAULT_KEY_DERIVE,
                        payload = base64UrlEncoder.encode(paraSodium.bytes(keyId.toString()))
                    )
            }

            null -> throw UnsupportedEncryptionSchemeException("algorithm: ${decodedKey.algorithm}")
        }
    }

    private fun encryptDataWithSymmetricKey(
        data: String,
        key: ByteArray
    ): ByteArray {
        val nonce = paraSodium.nonce(AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES)
        val messageBytes: ByteArray = paraSodium.bytes(data)
        val cipherBytes = ByteArray(AEAD.XCHACHA20POLY1305_IETF_ABYTES + messageBytes.size)

        val result = paraSodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
            cipher = cipherBytes,
            cipherLen = null,
            message = messageBytes,
            messageLen = messageBytes.size.toLong(),
            additionalData = null,
            additionalDataLen = 0,
            nSec = null,
            nPub = nonce,
            key = key
        )

        if (!result) {
            throw EncryptionException(message = "Could not encrypt message.")
        }

        return nonce.plus(cipherBytes)
    }

    override fun decryptDataWithSymmetricKey(data: String, key: String): String {
        val cipher = data.decodeAsCipherPayload()
        val decodedKey = key.decodeAsCipherPayload()

        val algorithmMatches = when (cipher.algorithm) {
            CipherAlgType.DEFAULT -> decodedKey.algorithm == CipherAlgType.DEFAULT || decodedKey.algorithm == CipherAlgType.DEFAULT_KEY_DERIVE
            CipherAlgType.DEFAULT_KEY_DERIVE -> decodedKey.algorithm == CipherAlgType.DEFAULT || decodedKey.algorithm == CipherAlgType.DEFAULT_KEY_DERIVE
            null -> decodedKey.algorithm == null
        }

        if (!algorithmMatches) {
            throw UnsupportedEncryptionSchemeException("algorithm mismatch")
        }

        when (cipher.algorithm) {
            CipherAlgType.DEFAULT -> {
                return decryptDataWithSymmetricKey(
                    cipher = cipher,
                    key = decodedKey.decodeData()
                )
            }

            CipherAlgType.DEFAULT_KEY_DERIVE -> {
                val keyId = cipher.payload
                    ?.let { paraSodium.str(base64UrlEncoder.decode(it)) }
                    ?.toLongOrNull()
                    ?: throw UnsupportedEncryptionSchemeException("keyId: ${cipher.payload}")
                val newKey = deriveSymmetricKey(
                    key = decodedKey.data,
                    derivedKeyId = keyId,
                    context = "_derive_"
                )
                return decryptDataWithSymmetricKey(
                    cipher = cipher,
                    key = newKey.decodeAsCipherPayload().decodeData()
                )
            }

            null -> throw UnsupportedEncryptionSchemeException("algorithm: ${cipher.algorithm}")
        }
    }

    private fun decryptDataWithSymmetricKey(cipher: CipherAlg, key: ByteArray): String {
        val source = cipher.decodeData()
        val nonce = source.sliceArray(0 until AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES)
        val cipherBytes =
            source.sliceArray(AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES until source.size)
        val messageBytes = ByteArray(cipherBytes.size - AEAD.XCHACHA20POLY1305_IETF_ABYTES)

        val result = paraSodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
            message = messageBytes,
            messageLen = null,
            nSec = null,
            cipher = cipherBytes,
            cipherLen = cipherBytes.size.toLong(),
            additionalData = null,
            additionalDataLen = 0L,
            nPub = nonce,
            key = key
        )
        if (!result) {
            throw DecryptionException(type = "symmetric", message = "Could not decrypt message.")
        }

        return paraSodium.str(messageBytes)
    }

    override fun deriveSymmetricKey(
        key: String,
        derivedKeyId: Long,
        context: String
    ): String {
        val decodedKey = key.decodeAsCipherPayload()
        when (decodedKey.algorithm) {
            CipherAlgType.DEFAULT, CipherAlgType.DEFAULT_KEY_DERIVE -> {
                val derivedKey = paraSodium.cryptoKdfDeriveFromKey(
                    KeyDerivation.MASTER_KEY_BYTES,
                    derivedKeyId,
                    context,
                    Key.fromBytes(decodedKey.decodeData())
                )

                return derivedKey.getAsBytes().encodeWithCipherAlg()
            }

            else -> throw UnsupportedEncryptionSchemeException(scheme = "algorithm ${decodedKey.algorithm}")
        }
    }

    override suspend fun deriveKeyFromPassword(
        password: String,
        salt: String,
        alg: KDFAlg
    ): String {
        when (alg.kdfType) {
            KDFType.ARGON2ID -> {
                val saltOutput = paraSodium.randomBytesBuf(PwHash.ARGON2ID_SALTBYTES)
                val saltInput = paraSodium.bytes(salt)
                if (
                    !paraSodium.cryptoGenericHash(
                        dataOut = saltOutput,
                        outLen = PwHash.ARGON2ID_SALTBYTES,
                        dataIn = saltInput,
                        inLen = saltInput.size.toLong()
                    )
                ) {
                    throw CryptoException("Error creating password context hash")
                }

                val passwordBytes: ByteArray = paraSodium.bytes(password)
                PwHash.checkAll(
                    passwordBytesLen = passwordBytes.size.toLong(),
                    saltBytesLen = saltOutput.size.toLong(),
                    opsLimit = alg.rounds,
                    memLimit = alg.memoryBytes
                )
                val outputKey = ByteArray(alg.keyBytes)
                val result = paraSodium.cryptoPwHash(
                    outputHash = outputKey,
                    outputHashLen = outputKey.size,
                    password = passwordBytes,
                    passwordLen = passwordBytes.size,
                    salt = saltOutput,
                    opsLimit = alg.rounds,
                    memLimit = alg.memoryBytes,
                    alg = PwHash.Alg.PWHASH_ALG_ARGON2ID13
                )

                if (!result) {
                    throw CryptoException(
                        message = "Could not hash your string. This may be due to insufficient " +
                                "memory or your CPU does not support Argon2's instruction set."
                    )
                }

                return when (alg.encodingType) {
                    EncodingType.HEX -> hexEncoder.encode(outputKey)
                    EncodingType.BASE64 -> base64Encoder.encode(outputKey)
                    EncodingType.BASE64_URL,
                    null -> base64UrlEncoder.encode(outputKey)
                }
            }

            null -> throw UnsupportedEncryptionSchemeException(scheme = alg.kdf)
        }
    }

    override fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {
        val decodedPrivateKey = myPrivateKey.decodeAsCipherPayload()
        val decodedPublicKey = publicKey.decodeAsCipherPayload()

        if (decodedPrivateKey.algorithm != decodedPublicKey.algorithm) {
            throw UnsupportedEncryptionSchemeException("algorithm mismatch")
        }

        when (decodedPublicKey.algorithm) {
            CipherAlgType.DEFAULT -> {
                val nonce = paraSodium.nonce(Box.CURVE25519XSALSA20POLY1305_NONCEBYTES)
                val messageBytes: ByteArray = paraSodium.bytes(data)
                val cipherBytes = ByteArray(Box.MACBYTES + messageBytes.size)

                val result: Boolean = paraSodium.cryptoBoxEasy(
                    cipherText = cipherBytes,
                    message = messageBytes,
                    messageLen = messageBytes.size.toLong(),
                    nonce = nonce,
                    publicKey = decodedPublicKey.decodeData(),
                    secretKey = decodedPrivateKey.decodeData()
                )

                if (!result) {
                    throw EncryptionException(message = "Could not encrypt your message.")
                }

                return nonce.plus(cipherBytes).encodeWithCipherAlg()
            }

            else -> throw UnsupportedEncryptionSchemeException("algorithm: ${decodedPublicKey.algorithm}")
        }
    }

    override fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {
        val cipher = data.decodeAsCipherPayload()
        val decodedPrivateKey = myPrivateKey.decodeAsCipherPayload()
        val decodedPublicKey = publicKey.decodeAsCipherPayload()

        if (decodedPrivateKey.algorithm != decodedPublicKey.algorithm) {
            throw UnsupportedEncryptionSchemeException("algorithm mismatch")
        }

        if (cipher.algorithm != decodedPublicKey.algorithm) {
            throw UnsupportedEncryptionSchemeException("algorithm mismatch")
        }

        when (cipher.algorithm) {
            CipherAlgType.DEFAULT -> {
                val source = cipher.decodeData()
                val nonce = source.sliceArray(0 until Box.CURVE25519XSALSA20POLY1305_NONCEBYTES)
                val content =
                    source.sliceArray(Box.CURVE25519XSALSA20POLY1305_NONCEBYTES until source.size)

                val message = ByteArray(content.size - Box.MACBYTES)
                val res: Boolean = paraSodium.cryptoBoxOpenEasy(
                    message = message,
                    cipherText = content,
                    cipherTextLen = content.size.toLong(),
                    nonce = nonce,
                    publicKey = decodedPublicKey.decodeData(),
                    secretKey = decodedPrivateKey.decodeData()
                )

                if (!res) {
                    throw DecryptionException(
                        type = "keyPair",
                        message = "Could not decrypt your message."
                    )
                }

                return paraSodium.str(message)
            }

            else -> throw UnsupportedEncryptionSchemeException("algorithm: ${cipher.algorithm}")
        }
    }

    override fun signData(data: String, myPrivateKey: String): String {
        val decodedPrivateKey = myPrivateKey.decodeAsCipherPayload()
        when (decodedPrivateKey.algorithm) {
            CipherAlgType.DEFAULT -> {
                val messageBytes = paraSodium.bytes(data)
                val signedMessage = paraSodium.randomBytesBuf(Sign.BYTES + messageBytes.size)
                val result = paraSodium.cryptoSign(
                    signedMessage = signedMessage,
                    message = messageBytes,
                    messageLen = messageBytes.size.toLong(),
                    secretKey = decodedPrivateKey.decodeData()
                )

                if (!result) {
                    throw CryptoException("Could not sign your message.")
                }

                return signedMessage.encodeWithCipherAlg()
            }

            else -> throw UnsupportedEncryptionSchemeException("algorithm: ${decodedPrivateKey.algorithm}")
        }
    }

    override fun verifySignature(data: String, publicKey: String): String? {
        val signedData = data.decodeAsCipherPayload()
        val decodedPublicKey = publicKey.decodeAsCipherPayload()

        if (signedData.algorithm != decodedPublicKey.algorithm) {
            throw UnsupportedEncryptionSchemeException("algorithm mismatch")
        }

        when (signedData.algorithm) {
            CipherAlgType.DEFAULT -> {

                val signedMessageBytes = signedData.decodeData()
                val publicKeyBytes = decodedPublicKey.decodeData()
                val messageBytes = paraSodium.randomBytesBuf(signedMessageBytes.size - Sign.BYTES)

                val result = paraSodium.cryptoSignOpen(
                    message = messageBytes,
                    signedMessage = signedMessageBytes,
                    signedMessageLen = signedMessageBytes.size.toLong(),
                    publicKey = publicKeyBytes
                )

                if (!result) {
                    return null
                }

                return paraSodium.str(messageBytes)
            }

            else -> throw UnsupportedEncryptionSchemeException("algorithm: ${signedData.algorithm}")
        }
    }

    override fun computeHash(data: String, type: HashType, encodingType: EncodingType): String {

        val message: ByteArray = paraSodium.bytes(data)
        val hash: ByteArray = paraSodium.randomBytesBuf(type.bytes)
        val res: Boolean = paraSodium.cryptoGenericHash(
            dataOut = hash,
            outLen = hash.size,
            dataIn = message,
            inLen = message.size.toLong()
        )

        if (!res) {
            throw CryptoException("Could not hash the message.")
        }

        return when (encodingType) {
            EncodingType.HEX -> hexEncoder.encode(hash)
            EncodingType.BASE64 -> base64Encoder.encode(hash)
            EncodingType.BASE64_URL -> base64UrlEncoder.encode(hash)
        }
    }

    private fun ByteArray.encodeWithCipherAlg(
        algType: CipherAlgType = CipherAlgType.DEFAULT,
        payload: String? = null
    ): String {
        return CipherAlg(
            algType = algType,
            encodingType = EncodingType.BASE64_URL,
            payload = payload,
            data = base64UrlEncoder.encode(this)
        ).encode(skipTagsIfDefault = true)
    }

    private fun String.decodeAsCipherPayload(): CipherAlg {
        return CipherAlg.decode(this)
    }

    private fun CipherAlg.decodeData(): ByteArray {
        return when (encodingType) {
            EncodingType.HEX -> hexEncoder.decode(this.data)
            EncodingType.BASE64 -> base64Encoder.decode(this.data)
            EncodingType.BASE64_URL -> base64UrlEncoder.decode(this.data)
            null -> throw UnsupportedEncryptionSchemeException("encoding: ${this.encodingType}")
        }
    }
}