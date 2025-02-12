package app.paralino.core.domain.user

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoAccountOperationV2
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.crypto.user.CryptoUserKeysRemoteSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.logg
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.presentation.login.SignUpStepException
import app.paralino.core.provider.isSocketTimeout
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

class CreateAccountUseCase constructor(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val cryptoAccountOperationV2: CryptoAccountOperationV2,
    private val userKeyRepository: CryptoUserKeysLocalSource,
    private val userKeysRemoteSource: CryptoUserKeysRemoteSource,
    private val logoutUseCase: LogoutUseCase,
    private val appPrefs: AppPrefs
): LimitedExecution() {

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
                if (it >= SignUpStep.LOG_USER_IN) {
                    appPrefs.guestEmailV2 = email
                    appPrefs.guestPasswordV2 = password
                }
            }
    }

    fun signUp(
        email: String,
        password: String,
    ): Flow<SignUpStep> {
        return flow {

            var currentStep: SignUpStep = SignUpStep.STARTING
            emit(SignUpStep.STARTING)

            val loginAlg = cryptoCore.getDefaultLoginKDFAlg()
            val authKey = cryptoAccountOperationV2.deriveUserAuthKey(
                email = email,
                password = password,
                alg = loginAlg
            )
            val user: UserMeRaw
            val session: UserSession

            try {
                // test encryption before proceeding
                cryptoAccountOperationV2.generateNewKeysForUser(
                    userId = email,
                    password = password,
                    loginAlg = loginAlg
                )

                currentStep = SignUpStep.CREATING_ACCOUNT
                emit(SignUpStep.CREATING_ACCOUNT)
                user = userRepo.remoteSignUp(email = email, password = authKey.loginKey)

                currentStep = SignUpStep.LOG_USER_IN
                emit(SignUpStep.LOG_USER_IN)

                session = try {
                    userRepo.login(email = email, password = authKey.loginKey)
                } catch (e: Exception) {
                    if ((e is UnauthorizedException && e.type == UserSessionMonitor.existingSessionError) || e.isSocketTimeout()) {
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
                throw SignUpStepException(currentStep, e)
            }

            // user is created at server and have a valid session

            currentStep = SignUpStep.GENERATING_KEYS
            emit(SignUpStep.GENERATING_KEYS)

            val keys = try {
                cryptoAccountOperationV2.generateNewKeysForUser(
                    userId = user.id,
                    password = password,
                    loginAlg = loginAlg
                )
            } catch (e: Exception) {
                throw userKeySetupFailed(user = user, session = session, step = currentStep, e = e)
            }

            try {
                val remoteKey = userKeysRemoteSource.createPersonalKey(
                    data = keys.encryptedKeyData,
                    removeOldKeys = true
                )
                userKeyRepository.saveUserKey(
                    key = remoteKey,
                    decryptedMainKey = keys.userPersonalKey
                )
            } catch (e: Exception) {
                // try once more if socket times out
                try {
                    val remoteKey = userKeysRemoteSource.createPersonalKey(
                        data = keys.encryptedKeyData,
                        removeOldKeys = true
                    )
                    userKeyRepository.saveUserKey(
                        key = remoteKey,
                        decryptedMainKey = keys.userPersonalKey
                    )
                } catch (e: Exception) {
                    throw userKeySetupFailed(
                        user = user,
                        session = session,
                        step = currentStep,
                        e = e
                    )
                }
            }

            // user and user keys are ready

            currentStep = SignUpStep.INIT_USER_DATA
            emit(SignUpStep.INIT_USER_DATA)

            delay(500)
            userRepo.initLocallyAfterSignUp(userMeRaw = user, userSession = session)

            currentStep = SignUpStep.ALL_DONE
            emit(SignUpStep.ALL_DONE)
        }
            .startProcessing("create_account")
            .catch {
                logg.e("Error signing up", it)
                throw it
            }
            .flowOn(Dispatchers.IO)
    }

    private suspend fun userKeySetupFailed(
        user: UserMeRaw,
        session: UserSession,
        step: SignUpStep,
        e: Throwable
    ): SignUpStepException {
        userKeyRepository.clearAllKeys()
        // key generation failed but user is created
        // we are saving user as is, so account recovery can take place later on
        appPrefs.accountRecoveryFlag = true
        userRepo.initLocallyAfterSignUp(user, session)

        return SignUpStepException(step, e)
    }
}