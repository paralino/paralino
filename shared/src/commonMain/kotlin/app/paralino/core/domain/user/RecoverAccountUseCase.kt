package app.paralino.core.domain.user

import app.paralino.core.appPrefs
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKey
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class RecoverAccountUseCase(
    private val userRepo: UserRepository,
    private val userKeyRepo: CryptoUserKeyRepository,
    private val cryptoCore: CryptoCore,
    private val keyMapper: UserKeyMapperCore,
    private val logoutUseCase: LogoutUseCase,
    private val changeCredential: ChangeCredentialsUseCase
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
                val signatureKeyPair = keyMapper.mapKeyPair(
                    model = it,
                    userKey = personalKeyData
                )

                // key is valid
                GeneratedUserKeys(
                    userPersonalKey = personalKeyData,
                    signatureKeyPair = signatureKeyPair,
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
            .onEach { appPrefs.temporaryMasterAuthKey = "" }
    }

    suspend fun recoverUserAccount(masterAuthKey: String, key: GeneratedUserKeys) {

        // private asymmetric key is encrypted with symmetric key and keys don't change here
        val authKey: UserAuthDerivedKey = BaseRemoteModule.formatter.decodeFromString(masterAuthKey)

        changeCredential.changeCredentials(
            userId = userRepo.cachedUserId,
            oldKey = key,
            oldMasterAuthKey = null,
            newMasterAuthKey = authKey,
            action = { _, _ -> }
        )
            .onEach {
                if (it == ChangeCredentialsStep.DONE) {
                    // at this point encryption keys are correct and updated and old keys are removed
                    // we can remove temporary password
                    appPrefs.temporaryMasterAuthKey = ""
                }
            }
            .collect()
    }
}