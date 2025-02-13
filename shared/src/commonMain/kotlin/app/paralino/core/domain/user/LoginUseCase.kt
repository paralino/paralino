package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKeyV1
import app.paralino.core.crypto.user.CryptoAccountOperationV1
import app.paralino.core.crypto.user.CryptoAccountOperationV2
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserCryptoKeyRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.presentation.login.SignUpStepException
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingUserKeyException
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class LoginUseCase constructor(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val cryptoAccountOperationV1: CryptoAccountOperationV1,
    private val cryptoAccountOperationV2: CryptoAccountOperationV2,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val userKeysRemoteSource: CryptoUserKeysRemoteSource,
    private val keyMapper: UserKeyMapperCore,
    private val logoutUseCase: LogoutUseCase,
    private val appPrefs: AppPrefs
) : LimitedExecution() {

    fun loginAsGuest(): Flow<SignUpStep> {
        return if (appPrefs.hasV2GuestAccount()) {
            login(
                email = appPrefs.guestEmailV2,
                password = appPrefs.guestPasswordV2
            )
        } else if (appPrefs.hasV1GuestAccount()) {
            loginAsV1(
                email = appPrefs.guestEmailV1,
                password = appPrefs.guestPasswordV1
            )
        } else {
            flow {
                throw SignUpStepException(
                    SignUpStep.STARTING,
                    IllegalStateException("No guest account")
                )
            }
        }
    }

    fun login(email: String, password: String): Flow<SignUpStep> {
        return flow {
            logg.d("login", "Start login - derive password")
            emit(SignUpStep.STARTING)

            val loginAlg = cryptoCore.getDefaultLoginKDFAlg()
            val authKey = cryptoAccountOperationV2.deriveUserAuthKey(
                email = email,
                password = password,
                alg = loginAlg
            )

            emitAll(
                loginProcess(
                    email = email,
                    loginKey = authKey.loginKey
                ) {
                    cryptoAccountOperationV2.decryptUserPersonalKey(
                        password = password,
                        userId = it.ownerUserId,
                        dataSalt = it.dataSalt!!,
                        encryptedMasterKey = it.encryptedSymmetricKey,
                        alg = BaseRemoteModule.formatter.decodeFromString(it.dataAlg)
                    )
                }
            )
        }
            .startProcessing("user_login")
            .catch {
                logg.e("login", "Login not successful", it)
                throw it
            }
    }

    private fun loginAsV1(email: String, password: String): Flow<SignUpStep> {
        return flow {
            logg.d("login", "Start login - derive password")
            emit(SignUpStep.STARTING)

            val authKey = getV1MasterAuthKey(email = email, password = password)

            emitAll(
                loginProcess(
                    email = email,
                    loginKey = authKey.loginKey
                ) {
                    cryptoAccountOperationV1.decryptUserPersonalKey(
                        masterAuthKey = authKey.masterAuthKey,
                        userId = it.ownerUserId,
                        encryptedMasterKey = it.encryptedSymmetricKey,
                        alg = BaseRemoteModule.formatter.decodeFromString(it.dataAlg)
                    )
                }
            )
        }
            .startProcessing("user_login")
            .catch {
                logg.e("login", "Login not successful", it)
                throw it
            }
    }

    private fun loginProcess(
        email: String,
        loginKey: String,
        getUserKey: suspend (UserCryptoKeyRaw) -> String
    ): Flow<SignUpStep> {
        return flow {

            val session: UserSession
            val user: UserMeRaw

            try {
                logg.d("login", "Start login to remote")

                emit(SignUpStep.LOG_USER_IN)
                session = try {
                    userRepo.login(email = email, password = loginKey)
                } catch (e: Exception) {
                    if (e is UnauthorizedException && e.type == UserSessionMonitor.existingSessionError) {
                        logg.w("login", "Cleanup any ghost session")

                        try {
                            logoutUseCase.fullLogout(logoutLocallyOnError = true)
                        } catch (e: Exception) {
                        }

                        userRepo.login(email = email, password = loginKey)
                    } else {
                        throw e
                    }
                }
                logg.d("login", "Session created")

                user = userRepo.getUserFromRemote()
                logg.d("login", "User fetched")
            } catch (e: Exception) {
                logg.w("login", "Could not login", e)
                throw SignUpStepException(SignUpStep.STARTING, e)
            }

            // user and session are ready
            // login details are correct at this point

            emit(SignUpStep.GENERATING_KEYS)
            val keyData: GeneratedUserKeys
            try {
                logg.d("login", "Fetch keys from remote")
                val keys = userKeysRemoteSource.getAllMyKeys()

                logg.d("login", "Found ${keys.size} key(s) on remote")

                val correctPersonalKey = keys
                    .sortedByDescending { it.userKeyVersion }
                    .firstNotNullOfOrNull {
                        try {
                            // try getting correct keys from encrypted data that matches password
                            val personalKeyData = getUserKey(it)

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
                            logg.w("login", "Error parsing key from remote", e)
                            null
                        }
                    }

                logg.d("login", "Found correct personal key: ${correctPersonalKey != null} of ${keys.size}")
                if (correctPersonalKey == null) {
                    // data is corrupted, current user password does not match user key
                    // this will trigger account recovery
                    appPrefs.accountRecoveryFlag = true
                    userRepo.initLocallyAfterSignUp(user, session)
                    throw MissingUserKeyException(false)
                } else {
                    keys.filter { it.id != correctPersonalKey.encryptedKeyData.id }
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            logg.d("login", "Delete (${it.size}) unused user keys")
                            // we have valid key and old unused keys are on server
                            // here we notify server to keep currently valid key
                            try {
                                userKeysRemoteSource.createPersonalKey(
                                    data = correctPersonalKey.encryptedKeyData,
                                    removeOldKeys = true
                                )
                            } catch (e: Exception) {
                                // ignore error if key is not deleted
                            }
                        }
                    keyData = correctPersonalKey
                }

            } catch (e: Exception) {
                // this will trigger account recovery
                logg.w("login", "Error initializing user keys", e)
                appPrefs.accountRecoveryFlag = true
                userRepo.initLocallyAfterSignUp(user, session)
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            // at this point we have both user and user key

            logg.d("login", "Start initializing user locally")
            emit(SignUpStep.INIT_USER_DATA)
            try {
                logg.d("login", "Save user keys locally")
                userKeyRepository.saveUserKey(
                    keyData.encryptedKeyData,
                    keyData.userPersonalKey
                )
                logg.d("login", "Save user object locally")
                userRepo.initLocallyAfterSignUp(user, session)
            } catch (e: Exception) {
                logg.w("login", "Error initializing local user", e)
                logoutUseCase.logoutLocally()
                throw SignUpStepException(SignUpStep.INIT_USER_DATA, e)
            }

            logg.d("login", "Login success")
            emit(SignUpStep.ALL_DONE)
        }
            .catch {
                logg.e("login", "Login not successful", it)
                throw it
            }
    }

    suspend fun getV1MasterAuthKey(
        email: String,
        password: String
    ): UserAuthDerivedKeyV1 {
        val loginDetails = userRepo.getLoginDetails(key = email)
        return cryptoAccountOperationV1.deriveUserAuthKey(
            salt = loginDetails.loginSalt,
            password = password,
            alg = loginDetails.loginAlg
        )
    }
}