package app.paralino.android.crpyto

import com.cossacklabs.themis.*
import app.paralino.core.crypto.Data
import app.paralino.core.crypto.ICryptoServiceProvider
import app.paralino.core.crypto.KeyPair
import app.paralino.core.crypto.toData
import app.paralino.core.logg

class CryptoFrameworkProvider : ICryptoServiceProvider {

    override fun createNewSymmetricKey(): Data {
        return SymmetricKey().toByteArray().toData()
    }

    override fun createNewAsymmetricKey(): KeyPair {
        val key = KeypairGenerator.generateKeypair()
        return KeyPair(key.privateKey.toByteArray(), key.publicKey.toByteArray())
    }

    override fun encryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray
    ): Data {
        logg.v("libs", "Start encrypt")

        val symKey = SymmetricKey(key)
        val cell = SecureCell.SealWithKey(symKey.toByteArray())
        val res = cell.encrypt(data).toData()

        logg.v("libs", "End encrypt")
        return res
    }

    override fun decryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray
    ): Data {
        logg.v("libs", "Start decrypt")

        val symKey = SymmetricKey(key)
        val cell = SecureCell.SealWithKey(symKey.toByteArray())
        val res = cell.decrypt(data).toData()

        logg.v("libs", "End decrypt")
        return res
    }

    override fun encryptDataWithPassword(data: ByteArray, password: String, context: ByteArray): Data {
        logg.v("libs", "Start encrypt with password")

        val cell = SecureCell.SealWithPassphrase(password)
        val res = cell.encrypt(data, context).toData()

        logg.v("libs", "End encrypt with password")
        return res
    }

    override fun decryptDataWithPassword(data: ByteArray, password: String, context: ByteArray): Data {
        logg.v("libs", "Start decrypt with password")
        val cell = SecureCell.SealWithPassphrase(password)
        val res = cell.decrypt(data, context).toData()

        logg.v("libs", "End decrypt with password")
        return res
    }

    override fun encryptDataWithKeyPair(data: ByteArray, myPrivateKey: ByteArray, publicKey: ByteArray): Data {
        val message = SecureMessage(PrivateKey(myPrivateKey))
        return message.wrap(data, PublicKey(publicKey)).toData()
    }

    override fun decryptDataWithKeyPair(data: ByteArray, myPrivateKey: ByteArray, publicKey: ByteArray): Data {
        val message = SecureMessage(PrivateKey(myPrivateKey))
        return message.unwrap(data, PublicKey(publicKey)).toData()
    }

    override fun signData(data: ByteArray, myPrivateKey: ByteArray): Data {
        val message = SecureMessage(PrivateKey(myPrivateKey))
        return message.sign(data).toData()
    }

    override fun verifySignature(data: ByteArray, publicKey: ByteArray): Data? {
        val message = SecureMessage(PublicKey(publicKey))
        return try {
            message.verify(data).toData()
        } catch (e: Exception) {
            null
        }
    }
}