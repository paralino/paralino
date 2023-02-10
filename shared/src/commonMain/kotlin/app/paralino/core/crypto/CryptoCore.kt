package app.paralino.core.crypto


import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.user.AsymmetricCryptoKeyData
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


class CryptoCore(
    val serviceProvider: ICryptoServiceProvider,
    val hashProvider: ICryptoHashProvider,
    val keyProvider: ICryptoKeyProvider,
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
        keyVersion: Long = nowUTC().toEpochMilliseconds(),
    ): GeneratedUserKeys {
        val newPersonalKey = getNewSymmetricKey()
        val encryptedPersonalKey = serviceProvider.encryptDataWithPassword(
            data = newPersonalKey.content,
            password = backupPassword,
            context = userId.toData(hashProvider).content
        )

        val newKeyPair = getNewAsymmetricKey()
        val encryptedPrivateKey = encryptDataWithKey(
            data = newKeyPair.privateKey.toData().asBase64String(hashProvider),
            key = newPersonalKey.content
        )
        val encryptedPublicKey = encryptDataWithKey(
            data = newKeyPair.publicKey.toData().asBase64String(hashProvider),
            key = newPersonalKey.content
        )

        val encryptedKeyData = UserCryptoKeyRaw(
            "",
            ownerUserId = userId,
            encryptedSymmetricKey = encryptedPersonalKey.asBase64String(hashProvider),
            encryptedPrivateKey = encryptedPrivateKey,
            encryptedPublicKey = encryptedPublicKey,
            version = keyVersion
        )

        return GeneratedUserKeys(newPersonalKey, encryptedKeyData)
    }

    fun generateNewAsymmetricUserKey(version: Long): AsymmetricCryptoKeyData {
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
        encryptedKey: String,
    ): String {
        return blocking {
            val userKey = serviceProvider.decryptDataWithPassword(
                data = encryptedKey.base64ToData(hashProvider).content,
                password = oldBackupPassword,
                context = userId.toData(hashProvider).content
            )

            reEncryptSymmetricKey(newBackupPassword, userId, userKey)
        }
    }

    suspend fun reEncryptSymmetricKey(
        backupPassword: String,
        userId: String,
        userKey: Data,
    ): String {
        return blocking {
            val newEncryptedUserKey = serviceProvider.encryptDataWithPassword(
                data = userKey.content,
                password = backupPassword,
                context = userId.toData(hashProvider).content
            )

            newEncryptedUserKey.asBase64String(hashProvider)
        }
    }


    suspend fun signIn(
        password: String,
        userId: String,
        keyData: String,
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

    suspend fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String,
    ): String {
        return serviceProvider.encryptDataWithKeyPair(
            data = data.toData(hashProvider).content,
            myPrivateKey = myPrivateKey.base64ToData(hashProvider).content,
            publicKey = publicKey.base64ToData(hashProvider).content
        ).asBase64String(hashProvider)
    }

    suspend fun encryptDataWithKeyPair(
        data: ByteArray,
        myPrivateKey: String,
        publicKey: String,
    ): String {
        return serviceProvider.encryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey.base64ToData(hashProvider).content,
            publicKey = publicKey.base64ToData(hashProvider).content
        ).asBase64String(hashProvider)
    }

    suspend fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String,
    ): String {
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

    fun signData(data: String, myPrivateKey: String): String {
        return signData(data, myPrivateKey.base64ToData(hashProvider).content)
    }

    fun signData(data: String, myPrivateKey: ByteArray): String {
        return signData(data.toData(hashProvider).content, myPrivateKey).asBase64String(hashProvider)
    }

    fun signData(data: ByteArray, myPrivateKey: ByteArray): Data {
        return serviceProvider.signData(data, myPrivateKey)
    }

    fun verifySignature(data: String, publicKey: String): String? {
        return verifySignature(data, publicKey.base64ToData(hashProvider).content)
    }

    fun verifySignature(data: String, publicKey: ByteArray): String? {
        return verifySignature(data.base64ToData(hashProvider).content, publicKey)?.asString(hashProvider)
    }

    fun verifySignature(data: ByteArray, publicKey: ByteArray): Data? {
        return serviceProvider.verifySignature(data, publicKey)
    }

    fun derivePassword(password: String): DerivedPasswords {
        val passwordData = password.toData(hashProvider).content

        val passwordHash = hashProvider.computeHash(passwordData, HashType.SHA256).content
        val sourceHash = hashProvider.computeHash(passwordHash, HashType.SHA512).content

        return DerivedPasswords(
            loginPassword = sourceHash.sliceArray(0 until 32).toData().asBase64String(hashProvider),
            backupPassword = sourceHash.sliceArray(32 until 64).toData()
                .asBase64String(hashProvider)
        )
    }

}

class GeneratedUserKeys(
    val userPersonalKey: Data,
    val encryptedKeyData: UserCryptoKeyRaw,
)