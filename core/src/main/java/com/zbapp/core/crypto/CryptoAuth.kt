package com.zbapp.core.crypto


import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.models.user.SymmetricCryptoKeyData
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.repo.base.IUserCrypto
import com.zbapp.core.utils.blocking


class CryptoAuth(
    val cryptoAuthProvider: ICryptoAuthProvider
) : IUserCrypto {

    suspend fun reEncryptSymmetricKey(
        oldBackupPassword: String,
        newBackupPassword: String,
        userId: String,
        keyData: SymmetricCryptoKeyData
    ): SymmetricCryptoKeyData {
        return blocking {
            val userKey = cryptoAuthProvider.decryptDataWithPassword(
                data = keyData.encryptedKey.base64ToByteArray(),
                password = oldBackupPassword,
                context = userId.asByteArray()
            )
            val newEncryptedUserKey = cryptoAuthProvider.encryptDataWithPassword(
                data = userKey,
                password = newBackupPassword,
                context = userId.asByteArray()
            )
            SymmetricCryptoKeyData(
                encryptedKey = newEncryptedUserKey.asBase64String(),
                version = keyData.version + 1
            )
        }
    }

    fun saveUserKeyLocally(key: ByteArray) {
        cryptoAuthProvider.saveUserPersonalKeyLocally(key)
    }

    fun clearUserPersonalKey() {
        cryptoAuthProvider.clearUserPersonalKey()
    }

    fun logout() {
        cryptoAuthProvider.clearAllUserKeys()
    }

    suspend fun signIn(
        password: String,
        userId: String,
        keyData: String
    ): ByteArray {
        return cryptoAuthProvider.decryptDataWithPassword(
            data = keyData.base64ToByteArray(),
            password = password,
            context = userId.asByteArray()
        )
    }

    suspend fun generateNewKeysForUser(
        backupPassword: String,
        userId: String,
        userData: UserMeData?
    ): GeneratedUserKeys {
        val newPersonalKey = cryptoAuthProvider.createNewSymmetricKey()
        val encryptedPersonalKey = cryptoAuthProvider.encryptDataWithPassword(
            data = newPersonalKey,
            password = backupPassword,
            context = userId.asByteArray()
        )

        val newKeyPair = cryptoAuthProvider.createNewAsymmetricKey()
        val encryptedPrivateKey = cryptoAuthProvider.encryptDataWithSymmetricKey(
            data = newKeyPair.privateKey,
            key = newPersonalKey,
            context = userId.asByteArray()
        )

        val symmetricData = SymmetricCryptoKeyData(
            encryptedKey = encryptedPersonalKey.asBase64String(),
            version = userData?.privateKeyBackup?.version?.plus(1) ?: 1
        )
        val asymmetricData = AsymmetricCryptoKeyData(
            privateKey = encryptedPrivateKey.asBase64String(),
            publicKey = newKeyPair.publicKey.asBase64String(),
            version = userData?.asymmetricKeys?.size?.plus(1) ?: 1
        )

        return GeneratedUserKeys(newPersonalKey, symmetricData, asymmetricData)
    }

    fun generateNewAsymmetricUserKey(user: UserMe): AsymmetricCryptoKeyData {
        val newKeyPair = cryptoAuthProvider.createNewAsymmetricKey()
        val encryptedPrivateKey = cryptoAuthProvider.encryptDataWithSymmetricKey(
            data = newKeyPair.privateKey,
            key = cryptoAuthProvider.loadUserPersonalKey(),
            context = user.id.asByteArray()
        )

        return AsymmetricCryptoKeyData(
            privateKey = encryptedPrivateKey.asBase64String(),
            publicKey = newKeyPair.publicKey.asBase64String(),
            version = user.data?.asymmetricKeys?.size?.plus(1) ?: 1
        )
    }

    fun getNewSymmetricKey(): ByteArray {
        return cryptoAuthProvider.createNewSymmetricKey()
    }

    override suspend fun encryptData(data: String): String {
        if (data.isEmpty()) return data
        return encryptDataWithKey(data, cryptoAuthProvider.loadUserPersonalKey())
    }

    override suspend fun decryptData(data: String): String {
        if (data.isEmpty()) return data
        return decryptDataWithKey(data, cryptoAuthProvider.loadUserPersonalKey())
    }

    suspend fun encryptDataWithKey(data: String, key: String): String {
        return encryptDataWithKey(data, key.base64ToByteArray())
    }

    suspend fun encryptDataWithKey(data: String, key: ByteArray): String {
        return encryptDataWithKey(data.asByteArray(), key)
    }

    suspend fun encryptDataWithKey(data: ByteArray, key: ByteArray): String {
        return cryptoAuthProvider.encryptDataWithSymmetricKey(
            data = data,
            key = key,
            context = null
        ).asBase64String()
    }

    suspend fun decryptDataWithKey(data: String, key: String): String {
        return decryptDataWithKey(data, key.base64ToByteArray())
    }

    suspend fun decryptDataWithKey(data: String, key: ByteArray): String {
        return decryptDataWithKey(data.base64ToByteArray(), key)
    }

    suspend fun decryptDataWithKey(data: ByteArray, key: ByteArray): String {
        return cryptoAuthProvider.decryptDataWithSymmetricKey(
            data = data,
            key = key,
            context = null
        ).asString()
    }

    suspend fun decryptDataWithPassword(data: String, password: String): String {
        return cryptoAuthProvider.decryptDataWithPassword(
            data = data.base64ToByteArray(),
            password = password,
            context = null
        ).asString()
    }

    suspend fun encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String {
        return cryptoAuthProvider.encryptDataWithKeyPair(
            data = data.asByteArray(),
            myPrivateKey = myPrivateKey.base64ToByteArray(),
            publicKey = publicKey.base64ToByteArray()
        ).asBase64String()
    }

    suspend fun encryptDataWithKeyPair(data: ByteArray, myPrivateKey: String, publicKey: String): String {
        return cryptoAuthProvider.encryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey.base64ToByteArray(),
            publicKey = publicKey.base64ToByteArray()
        ).asBase64String()
    }

    suspend fun decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String {
        return cryptoAuthProvider.decryptDataWithKeyPair(
            data = data.base64ToByteArray(),
            myPrivateKey = myPrivateKey.base64ToByteArray(),
            publicKey = publicKey.base64ToByteArray()
        ).asString()
    }

    private fun ByteArray.asString(): String {
        return cryptoAuthProvider.byteArrayToString(this)
    }

    private fun ByteArray.asBase64String(): String {
        return cryptoAuthProvider.toBase64(this)
    }

    private fun String.base64ToByteArray(): ByteArray {
        return cryptoAuthProvider.fromBase64(this)
    }

    private fun String.asByteArray(): ByteArray {
        return cryptoAuthProvider.toByteArray(this)
    }

    fun derivePassword(password: String): DerivedPasswords {
        val passwordData = password.asByteArray()

        val passwordHash = cryptoAuthProvider.computeHash(passwordData, HashType.SHA256)
        val sourceHash = cryptoAuthProvider.computeHash(passwordHash, HashType.SHA512)

        return DerivedPasswords(
            loginPassword = sourceHash.sliceArray(0 until 32).asBase64String(),
            backupPassword = sourceHash.sliceArray(32 until 64).asBase64String()
        )
    }
}

class GeneratedUserKeys(
    val userPersonalKey: ByteArray,
    val symmetricCryptoKeyData: SymmetricCryptoKeyData,
    val asymmetricCryptoKeyData: AsymmetricCryptoKeyData
)

data class DerivedPasswords(
    val loginPassword: String,
    val backupPassword: String
)
