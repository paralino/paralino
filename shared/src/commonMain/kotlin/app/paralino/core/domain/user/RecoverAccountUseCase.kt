package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class RecoverAccountUseCase(
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    val cryptoCore: CryptoCore,
    val keyMapper: UserKeyMapperCore,
    val logoutUseCase: LogoutUseCase,
) {

    suspend fun getCorrectUserKeyForPassword(
        keys: List<UserCryptoKeyRaw>,
        password: String,
    ): GeneratedUserKeys? {
        return getCorrectUserKeyForPassword(
            keys = keys,
            userId = userRepo.cachedUserId,
            password = password
        )
    }

    suspend fun getUserKeys(): List<UserCryptoKeyRaw> {
        return userKeyRepo.getRemoteKeys()
    }

    suspend fun getCorrectUserKeyForPassword(
        keys: List<UserCryptoKeyRaw>,
        userId: String,
        password: String,
    ): GeneratedUserKeys? {

        val derivedPassword = cryptoCore.derivePassword(password)
        val correctPersonalKey = keys.reversed().firstNotNullOfOrNull {
            try {
                // try getting correct keys from encrypted data that matches password
                val personalKeyData = cryptoCore.signIn(
                    password = derivedPassword.backupPassword,
                    userId = userId,
                    keyData = it.encryptedSymmetricKey
                )

                // check if keypair is valid by decrypting
                keyMapper.mapKeyPair(
                    model = it,
                    userKey = personalKeyData
                )

                // key is valid
                GeneratedUserKeys(
                    userPersonalKey = personalKeyData,
                    encryptedKeyData = it
                )
            } catch (e: Exception) {
                null
            }
        }

        return correctPersonalKey
    }

    fun resetUserAccount(backupPassword: String): Flow<Unit> {
        return flow {
            val newKey = cryptoCore.generateNewKeysForUser(
                backupPassword = backupPassword,
                userId = userRepo.cachedUserId
            )
            emitAll(logoutUseCase.resetAccount(newKey.encryptedKeyData))
        }
            .onEach { config.temporaryBackupPassword = "" }
    }

    fun recoverUserAccount(backupPassword: String, key: GeneratedUserKeys): Flow<Unit> {
        return flow {

            // only re-encrypting symmetric key
            // private asymmetric key is encrypted with symmetric key and keys don't change here
            val newEncryptedKey = try {
                cryptoCore.reEncryptSymmetricKey(
                    backupPassword = backupPassword,
                    userId = userRepo.cachedUserId,
                    userKey = key.userPersonalKey
                )
            } catch (e: Exception) {
                throw DecryptionException("Can't change encryption key", e)
            }

            // change version
            val newKey = key.encryptedKeyData.copy(
                encryptedSymmetricKey = newEncryptedKey,
                version = nowUTC().toEpochMilliseconds()
            )

            // upload and save key
            val remoteKey = userKeyRepo.uploadNewUserKey(newKey, removeOldKeys = true)
            userKeyRepo.saveUserKeyLocally(remoteKey, key.userPersonalKey)

            // at this point encryption keys are correct and updated and old keys are removed
            // we can remove temporary password

            config.temporaryBackupPassword = ""

            emit(Unit)
        }
    }

}