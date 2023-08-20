package app.paralino.core.crypto

import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.extensions.blocking
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.encodeToString

class CryptoCore(
    private val serviceProvider: ICryptoServiceProvider,
) {
    val hashProvider: ICryptoHashProvider = serviceProvider

    fun getNewSymmetricKey(): String {
        return serviceProvider.createNewSymmetricKey()
    }

    fun getNewCryptoKeyPair(): KeyPair {
        return serviceProvider.createNewCryptoKeyPair()
    }

    suspend fun generateNewKeysForUser(
        backupPassword: String,
        userId: String,
        keyVersion: Long = nowUTC().toEpochMilliseconds(),
    ): GeneratedUserKeys {
        val newPersonalKey = getNewSymmetricKey()
        val encryptedPersonalKey = serviceProvider.encryptDataWithPassword(
            data = newPersonalKey,
            password = backupPassword,
            context = userId
        )

        val newSignatureKeyPair = serviceProvider.createNewSignatureKeyPair()
        val encryptedPrivateIdentityKey = encryptDataWithKey(
            data = newSignatureKeyPair.privateKey,
            key = newPersonalKey
        )
        val encryptedPublicIdentityKey = encryptDataWithKey(
            data = newSignatureKeyPair.publicKey,
            key = newPersonalKey
        )

        val encryptedKeyData = UserCryptoKeyRaw(
            "",
            ownerUserId = userId,
            encryptedSymmetricKey = encryptedPersonalKey,
            encryptedPrivateKey = encryptedPrivateIdentityKey,
            encryptedPublicKey = encryptedPublicIdentityKey,
            version = keyVersion
        )

        val keys = GeneratedUserKeys(newPersonalKey, encryptedKeyData)

        verifyKeys(keys, newSignatureKeyPair, backupPassword, userId)

        return keys
    }

    private suspend fun verifyKeys(keys: GeneratedUserKeys, identityKeyPair: KeyPair, password: String, userId: String) {
        check(
            serviceProvider.decryptDataWithPassword(
                data = keys.encryptedKeyData.encryptedSymmetricKey,
                password = password,
                context = userId
            ).contentEquals(keys.userPersonalKey)
        )
        check(
            decryptDataWithKey(
                data = keys.encryptedKeyData.encryptedPrivateKey,
                key = keys.userPersonalKey
            ).contentEquals(identityKeyPair.privateKey)
        )
        check(
            decryptDataWithKey(
                data = keys.encryptedKeyData.encryptedPublicKey,
                key = keys.userPersonalKey
            ).contentEquals(identityKeyPair.publicKey)
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
                data = encryptedKey,
                password = oldBackupPassword,
                context = userId
            )

            reEncryptSymmetricKey(newBackupPassword, userId, userKey)
        }
    }

    suspend fun reEncryptSymmetricKey(
        backupPassword: String,
        userId: String,
        userKey: String,
    ): String {
        return blocking {
            serviceProvider.encryptDataWithPassword(
                data = userKey,
                password = backupPassword,
                context = userId
            )
        }
    }

    suspend fun signIn(
        password: String,
        userId: String,
        keyData: String,
    ): String {
        return serviceProvider.decryptDataWithPassword(
            data = keyData,
            password = password,
            context = userId
        )
    }

    suspend inline fun <reified T> encryptModel(data: T, key: String): String {
        return encryptDataWithKey(BaseRemoteModule.formatter.encodeToString(data), key)
    }

    suspend fun encryptDataWithKey(data: String, key: String): String {
        return serviceProvider.encryptDataWithSymmetricKey(
            data = data,
            key = key
        )
    }

    suspend fun encryptDataWithSimplePassword(data: String, password: String, userId: String): String {
        return serviceProvider.encryptDataWithPassword(
            data = data,
            password = password,
            context= userId
        )
    }

    suspend fun decryptDataWithSimplePassword(data: String, password: String, userId: String): String {
        return serviceProvider.decryptDataWithPassword(
            data = data,
            password = password,
            context= userId
        )
    }

    suspend fun decryptDataWithKey(data: String, key: String): String {
        return serviceProvider.decryptDataWithSymmetricKey(
            data = data,
            key = key
        )
    }

    suspend fun encryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String,
    ): String {
        return serviceProvider.encryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey,
            publicKey = publicKey
        )
    }

    suspend fun decryptDataWithKeyPair(
        data: String,
        myPrivateKey: String,
        publicKey: String,
    ): String {
        return serviceProvider.decryptDataWithKeyPair(
            data = data,
            myPrivateKey = myPrivateKey,
            publicKey = publicKey
        )
    }

    suspend fun decryptKeyWithKeyPair(key: String, myPrivateKey: String, publicKey: String): String {
        return decryptDataWithKeyPair(
            data = key,
            myPrivateKey = myPrivateKey,
            publicKey = publicKey
        )
    }

    fun signData(data: String, myPrivateKey: String): String {
        return serviceProvider.signData(data, myPrivateKey)
    }

    fun verifySignature(data: String, publicKey: String): String? {
        return serviceProvider.verifySignature(data, publicKey)
    }

    fun derivePassword(password: String): DerivedPasswords {

        val passwordHash = hashProvider.computeHash(password, HashType.SHA256)
        val sourceHash = hashProvider.computeHash(passwordHash, HashType.SHA512)

        return DerivedPasswords(
            loginPassword = sourceHash.slice(0 until (sourceHash.length / 2)),
            backupPassword = sourceHash.slice((sourceHash.length / 2) until sourceHash.length)
        )
    }

}

class GeneratedUserKeys(
    val userPersonalKey: String,
    val encryptedKeyData: UserCryptoKeyRaw,
)