package app.paralino.android.crpyto

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
import java.nio.charset.StandardCharsets

class CryptoFrameworkLibsodium : ICryptoServiceProvider {

    private val lazySodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8, HexMessageEncoder())

    override fun createNewSymmetricKey(): String {
        return lazySodium.cryptoSecretBoxKeygen().asHexString
    }

    override fun createNewCryptoKeyPair(): KeyPair {
        val keyPair = lazySodium.cryptoBoxKeypair()
        return KeyPair(
            privateKey = keyPair.secretKey.asHexString,
            publicKey = keyPair.publicKey.asHexString
        )
    }

    override fun createNewSignatureKeyPair(): KeyPair {
        val keyPair = lazySodium.cryptoSignKeypair()
        return KeyPair(
            privateKey = keyPair.secretKey.asHexString,
            publicKey = keyPair.publicKey.asHexString
        )
    }

    override fun encryptDataWithSymmetricKey(data: String, key: String): String {
        val nonce = lazySodium.nonce(SecretBox.XSALSA20POLY1305_NONCEBYTES)
        return lazySodium.cryptoSecretBoxEasy(
            data,
            nonce,
            Key.fromHexString(key)
        ).let { lazySodium.encodeToString(nonce).plus(it) }
    }

    override suspend fun decryptDataWithSymmetricKey(data: String, key: String): String {

        val source = lazySodium.decodeFromString(data)
        val nonce = source.sliceArray(0 until SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val content = source.sliceArray(SecretBox.XSALSA20POLY1305_NONCEBYTES until source.size)

        return lazySodium.cryptoSecretBoxOpenEasy(
            lazySodium.encodeToString(content),
            nonce,
            Key.fromHexString(key)
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

    override suspend fun decryptDataWithPassword(
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
            Key.fromHexString(publicKey),
            Key.fromHexString(myPrivateKey)
        )

        return lazySodium.cryptoBoxEasy(
            data,
            nonce,
            keyPair
        ).let { lazySodium.encodeToString(nonce).plus(it) }
    }

    override fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String
    ): String {

        val source = lazySodium.decodeFromString(data)
        val nonce = source.sliceArray(0 until SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val content = source.sliceArray(SecretBox.XSALSA20POLY1305_NONCEBYTES until source.size)

        val keyPair = com.goterl.lazysodium.utils.KeyPair(
            Key.fromHexString(publicKey),
            Key.fromHexString(myPrivateKey)
        )

        return lazySodium.cryptoBoxOpenEasy(
            lazySodium.encodeToString(content),
            nonce,
            keyPair
        )
    }

    override fun signData(data: String, myPrivateKey: String): String {
        return lazySodium.cryptoSign(
            data,
            Key.fromHexString(myPrivateKey)
        )
    }

    override fun verifySignature(data: String, publicKey: String): String? {
        return lazySodium.cryptoSignOpen(
            data,
            Key.fromHexString(publicKey)
        )
    }

    override fun computeHash(data: String, type: HashType): String {
        return when(type) {
            HashType.SHA256 -> lazySodium.cryptoHashSha256(data)
            HashType.SHA512 -> lazySodium.cryptoHashSha512(data)
        }
    }

}