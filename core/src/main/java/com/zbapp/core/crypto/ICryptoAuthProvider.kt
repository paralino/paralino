package com.zbapp.core.crypto

interface ICryptoAuthProvider {
    fun createNewSymmetricKey(): ByteArray
    fun createNewAsymmetricKey(): KeyPair
    fun encryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray,
        context: ByteArray?
    ): ByteArray

    fun decryptDataWithSymmetricKey(
        data: ByteArray,
        key: ByteArray,
        context: ByteArray?
    ): ByteArray

    fun encryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): ByteArray
    fun decryptDataWithPassword(data: ByteArray, password: String, context: ByteArray?): ByteArray
    fun saveUserPersonalKeyLocally(data: ByteArray)
    fun loadUserPersonalKey(): ByteArray
    fun clearUserPersonalKey()
    fun clearAllUserKeys()
    fun toBase64(data: ByteArray): String
    fun fromBase64(data: String): ByteArray
    fun toByteArray(data: String): ByteArray
    fun byteArrayToString(data: ByteArray): String
}

class KeyPair(val privateKey: ByteArray, val publicKey: ByteArray)