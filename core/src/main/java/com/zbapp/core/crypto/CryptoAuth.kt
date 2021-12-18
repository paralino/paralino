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
                data = keyData.key.base64ToByteArray(),
                password = oldBackupPassword,
                context = userId.asByteArray()
            )
            val newEncryptedUserKey = cryptoAuthProvider.encryptDataWithPassword(
                data = userKey,
                password = newBackupPassword,
                context = userId.asByteArray()
            )
            SymmetricCryptoKeyData(
                userId,
                key = newEncryptedUserKey.asBase64String(),
                version = keyData.version + 1
            )
        }
    }

    fun saveUserKeyLocally(key: ByteArray) {
        cryptoAuthProvider.saveUserPersonalKeyLocally(key)
    }

    fun logout() {
        cryptoAuthProvider.clearAllUserKeys()
    }

    suspend fun signIn(
        password: String,
        userId: String,
        keyData: String?
    ) {
        blocking {
            keyData?.let {
                val userKey = cryptoAuthProvider.decryptDataWithPassword(
                    data = it.base64ToByteArray(),
                    password = password,
                    context = userId.asByteArray()
                )
                cryptoAuthProvider.saveUserPersonalKeyLocally(userKey)
            }
        }
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
            identity = userId,
            key = encryptedPersonalKey.asBase64String(),
            version = userData?.privateKeyBackup?.version?.plus(1) ?: 1
        )
        val asymmetricData = AsymmetricCryptoKeyData(
            identity = userId,
            privateKey = encryptedPrivateKey.asBase64String(),
            publicKey = newKeyPair.publicKey.asBase64String(),
            version = userData?.asymmetricKeys?.size?.plus(1) ?: 1
        )

        return GeneratedUserKeys(newPersonalKey, symmetricData, asymmetricData)
    }

    fun generateNewAsymmetricUserKey(user: UserMe): AsymmetricCryptoKeyData {
        val newKeyPair = cryptoAuthProvider.createNewAsymmetricKey()
        return AsymmetricCryptoKeyData(
            identity = user.id,
            privateKey = newKeyPair.publicKey.asBase64String(),
            publicKey = newKeyPair.publicKey.asBase64String(),
            version = user.data?.asymmetricKeys?.size?.plus(1) ?: 1
        )
    }

    override suspend fun encryptData(data: String): String {
        if(data.isEmpty()) return data
        return encryptDataWithKey(data, cryptoAuthProvider.loadUserPersonalKey())
    }

    override suspend fun decryptData(data: String): String {
        if(data.isEmpty()) return data
        return decryptDataWithKey(data, cryptoAuthProvider.loadUserPersonalKey())
    }

    suspend fun encryptDataWithKey(data: String, key: ByteArray): String {
        return cryptoAuthProvider.encryptDataWithSymmetricKey(
            data = data.asByteArray(),
            key = key,
            context = null
        ).asBase64String()
    }

    suspend fun decryptDataWithKey(data: String, key: ByteArray): String {
        return cryptoAuthProvider.decryptDataWithSymmetricKey(
            data = data.base64ToByteArray(),
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

    companion object {
//        fun getDerivedPassword(password: String): DerivedPasswords {
//            return EThree.derivePasswords(password).let {
//                DerivedPasswords(it.loginPassword, it.backupPassword)
//            }
//        }
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

//fun Exception.isEThreeException(code: Int): Boolean {
//    return when(this) {
//        is EThreeException -> this.description.errorCode == code
//        is EThreeRatchetException -> this.description.errorCode == code
//        is FindUsersException -> this.description.errorCode == code
//        is FileGroupStorageException -> this.description.errorCode == code
//        is GroupException -> this.description.errorCode == code
//        is RawGroupException -> this.description.errorCode == code
//        is SQLiteStorageException -> this.description.errorCode == code
//        is TemporaryChannelException -> this.description.errorCode == code
//        else -> false
//    }
//}