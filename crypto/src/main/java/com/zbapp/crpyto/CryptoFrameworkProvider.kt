package com.zbapp.crpyto

import com.cossacklabs.themis.*
import com.zbapp.core.crypto.Data
import com.zbapp.core.crypto.ICryptoServiceProvider
import com.zbapp.core.crypto.toData

class CryptoFrameworkProvider : ICryptoServiceProvider {

    override fun encryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray,
        context: ByteArray?
    ): Data {
        val symKey = SymmetricKey(key)
        val cell = SecureCell.SealWithKey(symKey.toByteArray())
        return cell.encrypt(data, context).toData()
    }

    override fun decryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray,
        context: ByteArray?
    ): Data {
        val symKey = SymmetricKey(key)
        val cell = SecureCell.SealWithKey(symKey.toByteArray())
        return cell.decrypt(data, context).toData()
    }

    override fun encryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): Data {
        val cell = SecureCell.SealWithPassphrase(password)
        return cell.encrypt(data, context).toData()
    }

    override fun decryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): Data {
        val cell = SecureCell.SealWithPassphrase(password)
        return cell.decrypt(data, context).toData()
    }

    override fun encryptDataWithKeyPair(data: ByteArray, myPrivateKey: ByteArray, publicKey: ByteArray): Data {
        val message = SecureMessage(PrivateKey(myPrivateKey))
        return message.wrap(data, PublicKey(publicKey)).toData()
    }

    override fun decryptDataWithKeyPair(data: ByteArray, myPrivateKey: ByteArray, publicKey: ByteArray): Data {
        val message = SecureMessage(PrivateKey(myPrivateKey))
        return message.unwrap(data, PublicKey(publicKey)).toData()
    }

}