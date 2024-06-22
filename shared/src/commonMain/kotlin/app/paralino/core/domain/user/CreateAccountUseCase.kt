package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.UserAuthDerivedKey
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.presentation.login.SignUpStepException
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
    private val userKeyRepository: CryptoUserKeyRepository,
    private val logoutUseCase: LogoutUseCase
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
                    config.guestEmail = email
                    config.guestPassword = password
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

            try {
                val keys = createNewKey(userId = user.id, authKey = authKey)
                val remoteKey = userKeyRepository.uploadNewUserKey(keys.encryptedKeyData)
                userKeyRepository.saveUserKeyLocally(remoteKey, keys.userPersonalKey)
            } catch (e: Exception) {
                userKeyRepository.clearAll()
                // key generation failed but user is created
                // we are saving user as is, so account recovery can take place later on
                config.temporaryMasterAuthKey = authKey.masterAuthKey
                userRepo.initLocallyAfterSignUp(user, session)

                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
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