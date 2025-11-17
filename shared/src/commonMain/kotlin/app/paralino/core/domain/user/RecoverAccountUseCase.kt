package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.user.CryptoAccountOperationV1
import app.paralino.core.crypto.user.CryptoAccountOperationV2
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.SchemeVersion
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class RecoverAccountUseCase(
    private val userKeysRemoteSource: CryptoUserKeysRemoteSource,
    private val cryptoCore: CryptoCore,
    private val cryptoAccountOperationV1: CryptoAccountOperationV1,
    private val cryptoAccountOperationV2: CryptoAccountOperationV2,
    private val keyMapper: UserKeyMapperCore,
    private val logoutUseCase: LogoutUseCase,
    private val changeCredential: ChangeCredentialsUseCase,
    private val loginUseCase: LoginUseCase,
    private val userRepo: UserRepository,
    private val appPrefs: AppPrefs
) : LimitedExecution() {

    suspend fun getCorrectGuestUserKey(
        keys: List<UserCryptoKeyRaw>
    ): GeneratedUserKeys? {
        return getCorrectUserKeyForPassword(
            keys = keys,
            password = if (appPrefs.hasV1GuestAccount()) {
                appPrefs.guestPasswordV1
            } else if (appPrefs.hasV2GuestAccount()) {
                appPrefs.guestPasswordV2
            } else {
                throw IllegalStateException("Missing guest account")
            }
        )
    }

    suspend fun getCorrectUserKeyForPassword(
        keys: List<UserCryptoKeyRaw>,
        password: String,
    ): GeneratedUserKeys? {
        val correctPersonalKey = keys
            .sortedByDescending { it.userKeyVersion }
            .firstNotNullOfOrNull {
                try {
                    when (it.schemeVersionType) {
                        SchemeVersion.V1_0 -> {
                            // try getting correct keys from encrypted data that matches password
                            val authKey = loginUseCase.getV1MasterAuthKey(
                                email = appPrefs.guestEmailV1,
                                password = appPrefs.guestPasswordV1
                            )
                            val personalKeyData = cryptoAccountOperationV1.decryptUserPersonalKey(
                                masterAuthKey = authKey.masterAuthKey,
                                userId = appPrefs.userId,
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
                        }

                        SchemeVersion.V2_0 -> {
                            // try getting correct keys from encrypted data that matches password
                            val personalKeyData = cryptoAccountOperationV2.decryptUserPersonalKey(
                                password = password,
                                dataSalt = it.dataSalt!!,
                                userId = appPrefs.userId,
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
                        }

                        is SchemeVersion.UNKNOWN -> {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }

        return correctPersonalKey
    }

    suspend fun getUserKeysFromRemote(): List<UserCryptoKeyRaw> {
        return userKeysRemoteSource.getAllMyKeys()
    }

    fun resetGuestAccount(): Flow<Unit> {
        return flow {
            val newKey = if (appPrefs.hasV1GuestAccount()) {
                val authKey = loginUseCase.getV1MasterAuthKey(
                    email = appPrefs.guestEmailV1,
                    password = appPrefs.guestPasswordV1
                )
                cryptoAccountOperationV1.generateNewKeysForUser(
                    authKey = authKey,
                    userId = appPrefs.userId
                )
            } else if (appPrefs.hasV2GuestAccount()) {
                cryptoAccountOperationV2.generateNewKeysForUser(
                    loginAlg = cryptoCore.getDefaultLoginKDFAlg(),
                    password = appPrefs.guestPasswordV2,
                    userId = appPrefs.userId
                )
            } else {
                throw IllegalStateException("Missing guest account")
            }

            logoutUseCase.resetAccount(
                keyToPersist = newKey.encryptedKeyData,
                logoutLocallyOnError = false
            )
            emit(Unit)
        }
            .startProcessing("reset_user_account")
            .onEach { appPrefs.accountRecoveryFlag = false }
    }

    fun resetUserAccount(
        password: String
    ): Flow<Unit> {
        return flow {
            val newKey = cryptoAccountOperationV2.generateNewKeysForUser(
                loginAlg = cryptoCore.getDefaultLoginKDFAlg(),
                password = password,
                userId = appPrefs.userId
            )

            logoutUseCase.resetAccount(
                keyToPersist = newKey.encryptedKeyData,
                logoutLocallyOnError = false
            )
            emit(Unit)
        }
            .startProcessing("reset_user_account")
            .onEach { appPrefs.accountRecoveryFlag = false }
    }

    fun recoverGuestAccount(
        key: GeneratedUserKeys
    ): Flow<Unit> {
        val (email, password) = if (appPrefs.hasV1GuestAccount()) {
            appPrefs.guestEmailV1 to appPrefs.guestPasswordV1
        } else if (appPrefs.hasV2GuestAccount()) {
            appPrefs.guestEmailV2 to appPrefs.guestPasswordV2
        } else {
            throw IllegalStateException("Missing guest account")
        }

        return flow {
            // private asymmetric key is encrypted with symmetric key and keys don't change here
            changeCredential.changeCredentials(
                userId = appPrefs.userId,
                oldEmail = email,
                newEmail = email,
                oldPassword = password,
                newPassword = password,
                oldKeys = key,
                replaceKey = true,
                keepSessions = true,
                action = { oldLoginKey, newLoginKey ->
                    userRepo.changePassword(
                        oldPassword = oldLoginKey,
                        newPassword = newLoginKey
                    )

                    appPrefs.guestEmailV2 = email
                    appPrefs.guestPasswordV2 = password
                }
            )
                .onEach {
                    if (it == ChangeCredentialsStep.DONE) {
                        // at this point encryption keys are correct and updated and old keys are removed
                        // we can remove temporary password
                        appPrefs.accountRecoveryFlag = false
                    }
                }
                .collect()

            emit(Unit)
        }
            .startProcessing("recover_user_account")
    }

    fun recoverUserAccount(
        oldPassword: String,
        currentPassword: String,
        key: GeneratedUserKeys
    ): Flow<Unit> {
        return flow {
            // private asymmetric key is encrypted with symmetric key and keys don't change here
            changeCredential.changeCredentials(
                userId = appPrefs.userId,
                oldEmail = appPrefs.email,
                newEmail = appPrefs.email,
                oldPassword = oldPassword,
                newPassword = currentPassword,
                replaceKey = true,
                keepSessions = true,
                oldKeys = key,
                action = { oldLoginKey, newLoginKey ->
                    // using same password here to confirm recovery
                    // login credentials should not change, only key is pulled with old password
                    userRepo.changePassword(
                        oldPassword = newLoginKey,
                        newPassword = newLoginKey
                    )
                }
            )
                .onEach {
                    if (it == ChangeCredentialsStep.DONE) {
                        // at this point encryption keys are correct and updated and old keys are removed
                        // we can remove temporary password
                        appPrefs.accountRecoveryFlag = false
                    }
                }
                .collect()

            emit(Unit)
        }
            .startProcessing("recover_user_account")
    }
}