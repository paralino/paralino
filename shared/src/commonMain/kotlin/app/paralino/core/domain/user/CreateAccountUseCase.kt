package app.paralino.core.domain.user

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKey
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.logg
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.presentation.login.SignUpStepException
import app.paralino.core.provider.isSocketTimeout
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString

class CreateAccountUseCase constructor(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val userKeysRemoteSource: CryptoUserKeysRemoteSource,
    private val logoutUseCase: LogoutUseCase,
    private val appPrefs: AppPrefs
) {

    fun signUpAsGuest(): Flow<SignUpStep> {
        val email = "${
            cryptoCore.randomId(
                length = 32,
                alphabet = AlphabetType.EXTENDED
            )
        }${UserMe.guestUserEmail}"
        val password = cryptoCore.getNewSymmetricKey()
        return signUp(email, password)
            .onEach {
                if (it >= SignUpStep.GENERATING_KEYS) {
                    appPrefs.guestEmail = email
                    appPrefs.guestPassword = password
                }
            }
    }

    fun signUp(
        email: String,
        password: String,
    ): Flow<SignUpStep> {
        return flow {

            emit(SignUpStep.STARTING)
            val loginAlg = cryptoCore.getDefaultLoginKDFAlg()
            val authKey = cryptoCore.deriveUserAuthKey(
                salt = cryptoCore.randomId(
                    length = 32,
                    alphabet = AlphabetType.EXTENDED
                ),
                password = password,
                alg = loginAlg
            )
            val user: UserMeRaw
            val session: UserSession

            try {
                // test encryption before proceeding
                createNewKey(userId = email, authKey = authKey)

                user = userRepo.remoteSignUp(email = email, password = authKey.loginKey)

                session = try {
                    userRepo.login(email = email, password = authKey.loginKey)
                } catch (e: Exception) {
                    if (e is UnauthorizedException && e.type == UserSessionMonitor.existingSessionError) {
                        logg.w("login", "Cleanup any ghost session")

                        try {
                            logoutUseCase.fullLogout(logoutLocallyOnError = true)
                        } catch (e: Exception) {
                        }

                        userRepo.login(email = email, password = authKey.loginKey)
                    } else {
                        throw e
                    }
                }
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.STARTING, e)
            }

            // user is created at server and have a valid session

            emit(SignUpStep.GENERATING_KEYS)

            val keys = try {
                createNewKey(userId = user.id, authKey = authKey)
            } catch (e: Exception) {
                throw userKeySetupFailed(user, session, authKey, e)
            }

            try {
                val remoteKey = userKeysRemoteSource.createPersonalKey(
                    data = keys.encryptedKeyData,
                    removeOldKeys = true
                )
                userKeyRepository.saveUserKey(remoteKey, keys.userPersonalKey)
            } catch (e: Exception) {
                if (e.isSocketTimeout()) {
                    // try once more if socket times out
                    try {
                        val remoteKey = userKeysRemoteSource.createPersonalKey(
                            data = keys.encryptedKeyData,
                            removeOldKeys = true
                        )
                        userKeyRepository.saveUserKey(remoteKey, keys.userPersonalKey)
                    } catch (e: Exception) {
                        throw userKeySetupFailed(user, session, authKey, e)
                    }
                } else {
                    throw userKeySetupFailed(user, session, authKey, e)
                }
            }

            // user and user keys are ready

            emit(SignUpStep.INIT_USER_DATA)
            delay(500)
            userRepo.initLocallyAfterSignUp(user, session)

            emit(SignUpStep.ALL_DONE)
        }
            .catch {
                logg.e("Error signing up", it)
                throw it
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun userKeySetupFailed(
        user: UserMeRaw,
        session: UserSession,
        authKey: UserAuthDerivedKey,
        e: Throwable
    ): SignUpStepException {
        userKeyRepository.clearAllKeys()
        // key generation failed but user is created
        // we are saving user as is, so account recovery can take place later on
        appPrefs.temporaryMasterAuthKey = BaseRemoteModule.formatter.encodeToString(authKey)
        userRepo.initLocallyAfterSignUp(user, session)

        return SignUpStepException(SignUpStep.GENERATING_KEYS, e)
    }

    private suspend fun createNewKey(
        userId: String,
        authKey: UserAuthDerivedKey
    ): GeneratedUserKeys {
        return cryptoCore.generateNewKeysForUser(
            authKey = authKey,
            userId = userId
        )
    }
}