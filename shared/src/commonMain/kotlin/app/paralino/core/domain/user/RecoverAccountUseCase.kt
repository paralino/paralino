package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKey
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class RecoverAccountUseCase(
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoCore: CryptoCore,
    private val keyMapper: UserKeyMapperCore,
    private val logoutUseCase: LogoutUseCase,
) {

    suspend fun getCorrectUserKeyForPassword(
        keys: List<UserCryptoKeyRaw>,
        password: String,
    ): GeneratedUserKeys? {
        return getCorrectUserKeyForPassword(
            keys = keys,
            userId = userRepo.cachedUserId,
            userEmail = config.email,
            password = password
        )
    }

    suspend fun getUserKeys(): List<UserCryptoKeyRaw> {
        return userKeyRepo.getRemoteKeys()
    }

    suspend fun getCorrectUserKeyForPassword(
        keys: List<UserCryptoKeyRaw>,
        userId: String,
        userEmail: String,
        password: String,
    ): GeneratedUserKeys? {

        val correctPersonalKey = keys.reversed().firstNotNullOfOrNull {
            try {
                val authKey =
                    cryptoCore.deriveUserAuthKey(
                        salt = it.loginSalt,
                        password = password,
                        alg = BaseRemoteModule.formatter.decodeFromString(it.loginAlg)
                    )
                // try getting correct keys from encrypted data that matches password
                val personalKeyData = cryptoCore.decryptUserPersonalKey(
                    masterAuthKey = authKey.masterAuthKey,
                    userId = userId,
                    encryptedMasterKey = it.encryptedSymmetricKey,
                    alg = BaseRemoteModule.formatter.decodeFromString(it.dataAlg)
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

    fun resetUserAccount(masterAuthKey: String): Flow<Unit> {
        return flow {
            val newKey = cryptoCore.generateNewKeysForUser(
                authKey = BaseRemoteModule.formatter.decodeFromString(masterAuthKey),
                userId = userRepo.cachedUserId
            )
            logoutUseCase.resetAccount(
                keyToPersist = newKey.encryptedKeyData,
                logoutLocallyOnError = false
            )
            emit(Unit)
        }
            .onEach { config.temporaryMasterAuthKey = "" }
    }

    fun recoverUserAccount(masterAuthKey: String, key: GeneratedUserKeys): Flow<Unit> {
        return flow {

            // only re-encrypting symmetric key
            // private asymmetric key is encrypted with symmetric key and keys don't change here
            val dataKdfAlg = cryptoCore.getDefaultDataKDFAlg()
            val authKey: UserAuthDerivedKey = BaseRemoteModule.formatter.decodeFromString(masterAuthKey)
            val newEncryptedKey = try {
                cryptoCore.encryptUserPersonalKey(
                    userId = userRepo.cachedUserId,
                    masterAuthKey = authKey.masterAuthKey,
                    userPersonalKey = key.userPersonalKey,
                    alg = dataKdfAlg
                )
            } catch (e: Exception) {
                throw DecryptionException("Can't change encryption key", e)
            }

            // change version
            val newKey = key.encryptedKeyData.copy(
                encryptedSymmetricKey = newEncryptedKey,
                version = nowUTC().toEpochMilliseconds()
            )

            UserCryptoKeyRaw(
                id = key.encryptedKeyData.id,
                version = nowUTC().toEpochMilliseconds(),
                ownerUserId = key.encryptedKeyData.ownerUserId,
                encryptedSymmetricKey = newEncryptedKey,
                encryptedPrivateKey = key.encryptedKeyData.encryptedPrivateKey,
                encryptedPublicKey = key.encryptedKeyData.encryptedPublicKey,
                loginSalt = key.encryptedKeyData.loginSalt,
                loginAlg = BaseRemoteModule.formatter.encodeToString(key.encryptedKeyData.loginAlg),
                dataAlg = BaseRemoteModule.formatter.encodeToString(dataKdfAlg)
            )

            // upload and save key
            val remoteKey = userKeyRepo.uploadNewUserKey(newKey, removeOldKeys = true)
            userKeyRepo.saveUserKeyLocally(remoteKey, key.userPersonalKey)

            // at this point encryption keys are correct and updated and old keys are removed
            // we can remove temporary password

            config.temporaryMasterAuthKey = ""

            emit(Unit)
        }
    }
}