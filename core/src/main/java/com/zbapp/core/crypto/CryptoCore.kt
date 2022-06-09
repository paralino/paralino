package com.zbapp.core.crypto


import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.models.user.SymmetricCryptoKeyData
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.extensions.blocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


class CryptoCore(
    val serviceProvider: ICryptoServiceProvider,
    val hashProvider: ICryptoHashProvider,
    val keyProvider: ICryptoKeyProvider
) {

    fun getNewSymmetricKey(): Data {
        return keyProvider.createNewSymmetricKey()
    }

    fun getNewAsymmetricKey(): KeyPair {
        return keyProvider.createNewAsymmetricKey()
    }

    suspend fun generateNewKeysForUser(
        backupPassword: String,
        userId: String,
        privateKeyVersion: Int = 1,
        keyPairVersion: Int = 1
    ): GeneratedUserKeys {
        val newPersonalKey = getNewSymmetricKey().content
        val encryptedPersonalKey = serviceProvider.encryptDataWithPassword(
            data = newPersonalKey,
            password = backupPassword,
            context = userId.toData(hashProvider).content
        )

        val symmetricData = SymmetricCryptoKeyData(
            encryptedKey = encryptedPersonalKey.asBase64String(hashProvider),
            version = privateKeyVersion
        )
        val asymmetricData = generateNewAsymmetricUserKey(keyPairVersion)

        return GeneratedUserKeys(newPersonalKey, symmetricData, asymmetricData)
    }

    fun generateNewAsymmetricUserKey(version: Int = 1): AsymmetricCryptoKeyData {
        val newKeyPair = getNewAsymmetricKey()

        return AsymmetricCryptoKeyData(
            privateKey = newKeyPair.privateKey.toData().asBase64String(hashProvider),
            publicKey = newKeyPair.publicKey.toData().asBase64String(hashProvider),
            version = version
        )
    }

    suspend fun reEncryptSymmetricKey(
        oldBackupPassword: String,
        newBackupPassword: String,
        userId: String,
        keyData: SymmetricCryptoKeyData
    ): SymmetricCryptoKeyData {
        return blocking {
            val userKey = serviceProvider.decryptDataWithPassword(
                data = keyData.encryptedKey.base64ToData(hashProvider).content,
                password = oldBackupPassword,
                context = userId.toData(hashProvider).content
            )
            val newEncryptedUserKey = serviceProvider.encryptDataWithPassword(
                data = userKey.content,
                password = newBackupPassword,
                context = userId.toData(hashProvider).content
            )
            SymmetricCryptoKeyData(
                encryptedKey = newEncryptedUserKey.asBase64String(hashProvider),
                version = keyData.version + 1
            )
        }
    }


    suspend fun signIn(
        password: String,
        userId: String,
        keyData: String
    ): Data {
        return serviceProvider.decryptDataWithPassword(
            data = keyData.base64ToData(hashProvider).content,
            password = password,
            context = userId.toData(hashProvider).content
        )
    }

    suspend inline fun <reified T> encryptModel(data: T, key: ByteArray): String {
        return encryptDataWithKey(BaseRemoteModule.formatter.encodeToString(data), key)
    }

    suspend inline fun <reified T> decryptModel(data: String, key: String): T {
        return decryptModel(data, key.base64ToData(hashProvider).content)
    }

    suspend inline fun <reified T> decryptModel(data: String, key: ByteArray): T {
        return BaseRemoteModule.formatter.decodeFromString(decryptDataWithKey(data, key))
    }

    suspend fun encryptDataWithKey(data: String, key: String): String {
        return encryptDataWithKey(data, key.base64ToData(hashProvider).content)
    }

    suspend fun encryptDataWithKey(data: String, key: ByteArray): String {
        return encryptDataWithKey(data.toData(hashProvider).content, key)
    }

    suspend fun encryptDataWithKey(data: ByteArray, key: ByteArray): String {
        return serviceProvider.encryptDataWithSymmetricKey(
            data = data,
            key = key,
            context = null
        ).asBase64String(hashProvider)
    }

    suspend fun encryptDataWithSimplePassword(data: String, password: String): String {
        return serviceProvider.encryptDataWithPassword(
            data = data.toData(hashProvider).content,
            password = password,
            context = null
        ).asBase64String(hashProvider)
    }

    suspend fun decryptDataWithSimplePassword(data: String, password: String): String {
        return serviceProvider.decryptDataWithPassword(
            data = data.base64ToData(hashProvider).content,
            password = password,
            context = null
        ).asString(hashProvider)
    }

    suspend fun decryptDataWithKey(data: String, key: String): String {
        return decryptDataWithKey(data, key.base64ToData(hashProvider).content)
    }

    suspend fun decryptDataWithKey(data: String, key: ByteArray): String {
        return decryptDataWithKey(data.base64ToData(hashProvider).content, key)
    }

    suspend fun decryptDataWithKey(data: ByteArray, key: ByteArray): String {
        return serviceProvider.decryptDataWithSymmetricKey(
            data = data,
            key = key,
            context = null
        ).asString(hashProvider)
    }

    suspend fun decryptDataWithPassword(data: String, password: String): String {
        return serviceProvider.decryptDataWithPassword(
            data = data.base64ToData(hashProvider).content,
            password = password,
            context = null
        ).asString(hashProvider)
    }

    suspend fun encryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String {
        return serviceProvider.encryptDataWithKeyPair(
            data = data.toData(hashProvider).content,
            myPrivateKey = myPrivateKey.base64ToData(hashProvider).content,
            publicKey = publicKey.base64ToData(hashProvider).content
        ).asBase64String(hashProvider)
    }

    suspend fun encryptDataWithKeyPair(data: ByteArray, myPrivateKey: String, publicKey: String): String {
        return serviceProvider.encryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey.base64ToData(hashProvider).content,
            publicKey = publicKey.base64ToData(hashProvider).content
        ).asBase64String(hashProvider)
    }

    suspend fun decryptDataWithKeyPair(data: String, myPrivateKey: String, publicKey: String): String {
        return serviceProvider.decryptDataWithKeyPair(
            data = data.base64ToData(hashProvider).content,
            myPrivateKey = myPrivateKey.base64ToData(hashProvider).content,
            publicKey = publicKey.base64ToData(hashProvider).content
        ).asString(hashProvider)
    }

    suspend fun decryptKeyWithKeyPair(key: String, myPrivateKey: String, publicKey: String): Data {
        return serviceProvider.decryptDataWithKeyPair(
            data = key.base64ToData(hashProvider).content,
            myPrivateKey = myPrivateKey.base64ToData(hashProvider).content,
            publicKey = publicKey.base64ToData(hashProvider).content
        )
    }

    fun derivePassword(password: String): DerivedPasswords {
        val passwordData = password.toData(hashProvider).content

        val passwordHash = hashProvider.computeHash(passwordData, HashType.SHA256).content
        val sourceHash = hashProvider.computeHash(passwordHash, HashType.SHA512).content

        return DerivedPasswords(
            loginPassword = sourceHash.sliceArray(0 until 32).toData().asBase64String(hashProvider),
            backupPassword = sourceHash.sliceArray(32 until 64).toData().asBase64String(hashProvider)
        )
    }

}

class GeneratedUserKeys(
    val userPersonalKey: ByteArray,
    val symmetricCryptoKeyData: SymmetricCryptoKeyData,
    val asymmetricCryptoKeyData: AsymmetricCryptoKeyData
)