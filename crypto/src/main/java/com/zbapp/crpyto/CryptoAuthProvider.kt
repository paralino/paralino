package com.zbapp.crpyto

import android.util.Base64
import com.cossacklabs.themis.KeypairGenerator
import com.cossacklabs.themis.SecureCell
import com.cossacklabs.themis.SymmetricKey
import com.zbapp.core.config
import com.zbapp.core.crypto.ICryptoAuthProvider
import com.zbapp.core.crypto.KeyPair

class CryptoAuthProvider : ICryptoAuthProvider {

    override fun createNewSymmetricKey(): ByteArray {
        return SymmetricKey().toByteArray()
    }

    override fun createNewAsymmetricKey(): KeyPair {
        val key = KeypairGenerator.generateKeypair()
        return KeyPair(key.privateKey.toByteArray(), key.publicKey.toByteArray())
    }

    override fun encryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray,
        context: ByteArray?
    ): ByteArray {
        val symKey = SymmetricKey(key)
        val cell = SecureCell.SealWithKey(symKey.toByteArray())
        return cell.encrypt(data, context)
    }

    override fun decryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray,
        context: ByteArray?
    ): ByteArray {
        val symKey = SymmetricKey(key)
        val cell = SecureCell.SealWithKey(symKey.toByteArray())
        return cell.decrypt(data, context)
    }

    override fun encryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): ByteArray {
        val cell = SecureCell.SealWithPassphrase(password)
        return cell.encrypt(data, context)
    }

    override fun decryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): ByteArray {
        val cell = SecureCell.SealWithPassphrase(password)
        return cell.decrypt(data, context)
    }

    override fun saveUserPersonalKeyLocally(data: ByteArray) {
        config.userKey = toBase64(data)
    }

    override fun loadUserPersonalKey(): ByteArray {
        return fromBase64(config.userKey)
    }

    override fun clearUserPersonalKey() {
        config.userKey = ""
    }

    override fun clearAllUserKeys() {
        // TODO delete all user keys from local storage
        config.userKey = ""
    }

    override fun toBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    override fun fromBase64(data: String): ByteArray {
        return Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    override fun toByteArray(data: String): ByteArray {
        return data.encodeToByteArray(throwOnInvalidSequence = true)
    }

    override fun byteArrayToString(data: ByteArray): String {
        return data.decodeToString(throwOnInvalidSequence = true)
    }


}