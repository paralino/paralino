package app.paralino.android.crpyto

import app.paralino.core.crypto.Data
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KeyPair
import app.paralino.core.crypto.toData
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.GenericHash
import com.goterl.lazysodium.interfaces.PwHash
import com.goterl.lazysodium.interfaces.SecretBox
import com.goterl.lazysodium.utils.Key
import java.nio.charset.StandardCharsets

class CryptoFrameworkLibsodium : ICryptoServiceProvider {

    val lazySodium = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    override fun createNewSymmetricKey(): Data {
        return lazySodium.cryptoSecretBoxKeygen().asBytes.toData()
    }

    override fun createNewAsymmetricKey(): KeyPair {
        val keyPair = lazySodium.cryptoSignKeypair()
        return KeyPair(
            privateKey = keyPair.secretKey.asBytes,
            publicKey = keyPair.publicKey.asBytes
        )
    }

    override fun encryptDataWithSymmetricKey(data: ByteArray, key: ByteArray): Data {

        val nonce = lazySodium.nonce(SecretBox.XSALSA20POLY1305_NONCEBYTES)

        return lazySodium.cryptoSecretBoxEasy(
            lazySodium.encodeToString(data),
            nonce,
            Key.fromBytes(key)
        ).let { nonce.plus(lazySodium.decodeFromString(it)).toData() }
    }

    override fun decryptDataWithSymmetricKey(data: ByteArray, key: ByteArray): Data {

        val nonce = data.sliceArray(0 until SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val content = data.sliceArray(SecretBox.XSALSA20POLY1305_NONCEBYTES until data.size)

        return lazySodium.cryptoSecretBoxOpenEasy(
            lazySodium.encodeToString(content),
            nonce,
            Key.fromBytes(key)
        ).let { lazySodium.decodeFromString(it).toData() }
    }

    override fun encryptDataWithPassword(
        data: ByteArray,
        password: String,
        context: ByteArray
    ): Data {

        val out = lazySodium.randomBytesBuf(GenericHash.BYTES_MIN)
        if (!lazySodium.cryptoGenericHash(
                out,
                GenericHash.BYTES_MIN,
                context,
                context.size.toLong()
            )
        ) {
            throw SodiumException("Error creating password context hash")
        }

        val key = lazySodium.cryptoPwHash(
            password,
            PwHash.ARGON2ID_STR_BYTES,
            out,
            PwHash.OPSLIMIT_INTERACTIVE,
            PwHash.MEMLIMIT_INTERACTIVE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )

        return encryptDataWithSymmetricKey(data, lazySodium.decodeFromString(key))
    }

    override fun decryptDataWithPassword(
        data: ByteArray,
        password: String,
        context: ByteArray
    ): Data {

        val out = lazySodium.randomBytesBuf(GenericHash.BYTES_MIN)
        if (!lazySodium.cryptoGenericHash(
                out,
                GenericHash.BYTES_MIN,
                context,
                context.size.toLong()
            )
        ) {
            throw SodiumException("Error creating password context hash")
        }

        val key = lazySodium.cryptoPwHash(
            password,
            PwHash.ARGON2ID_STR_BYTES,
            out,
            PwHash.OPSLIMIT_INTERACTIVE,
            PwHash.MEMLIMIT_INTERACTIVE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )

        return decryptDataWithSymmetricKey(data, lazySodium.decodeFromString(key))
    }

    override fun encryptDataWithKeyPair(
        data: ByteArray,
        myPrivateKey: ByteArray,
        publicKey: ByteArray
    ): Data {
        val nonce = lazySodium.nonce(SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val keyPair = com.goterl.lazysodium.utils.KeyPair(
            Key.fromBytes(publicKey),
            Key.fromBytes(myPrivateKey)
        )
        val cryptoKeyPair = lazySodium.convertKeyPairEd25519ToCurve25519(keyPair)

        return lazySodium.cryptoBoxEasy(
            lazySodium.encodeToString(data),
            nonce,
            cryptoKeyPair
        ).let { nonce.plus(lazySodium.decodeFromString(it)).toData() }
    }

    override fun decryptDataWithKeyPair(
        data: ByteArray,
        myPrivateKey: ByteArray,
        publicKey: ByteArray
    ): Data {

        val nonce = data.sliceArray(0 until SecretBox.XSALSA20POLY1305_NONCEBYTES)
        val content = data.sliceArray(SecretBox.XSALSA20POLY1305_NONCEBYTES until data.size)
        val keyPair = com.goterl.lazysodium.utils.KeyPair(
            Key.fromBytes(publicKey),
            Key.fromBytes(myPrivateKey)
        )
        val cryptoKeyPair = lazySodium.convertKeyPairEd25519ToCurve25519(keyPair)

        return lazySodium.cryptoBoxOpenEasy(
            lazySodium.encodeToString(content),
            nonce,
            cryptoKeyPair
        ).let { lazySodium.decodeFromString(it).toData() }
    }

    override fun signData(data: ByteArray, myPrivateKey: ByteArray): Data {
        return lazySodium.cryptoSign(
            lazySodium.encodeToString(data),
            Key.fromBytes(myPrivateKey)
        ).let { lazySodium.decodeFromString(it).toData() }
    }

    override fun verifySignature(data: ByteArray, publicKey: ByteArray): Data? {
        return lazySodium.cryptoSignOpen(
            lazySodium.encodeToString(data),
            Key.fromBytes(publicKey)
        )?.let { lazySodium.decodeFromString(it).toData() }
    }

}