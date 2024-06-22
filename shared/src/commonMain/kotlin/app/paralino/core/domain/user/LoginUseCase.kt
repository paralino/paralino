package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.GeneratedUserKeys
import app.paralino.core.crypto.user.CryptoUserKeyRepository
import app.paralino.core.logg
import app.paralino.core.models.mappers.UserKeyMapperCore
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserSession
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.presentation.login.SignUpStepException
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.MissingUserKeyException
import app.paralino.core.utils.exceptions.UnauthorizedException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString

class LoginUseCase constructor(
    private val userRepo: UserRepository,
    private val cryptoCore: CryptoCore,
    private val userKeyRepository: CryptoUserKeyRepository,
    private val keyMapper: UserKeyMapperCore,
    private val logoutUseCase: LogoutUseCase,
) {

    fun login(email: String, password: String): Flow<SignUpStep> {
        return flow {

            logg.d("login", "Start login - derive password")
            emit(SignUpStep.STARTING)

            val loginDetails = userRepo.getLoginDetails(key = email)
            val authKey = cryptoCore.deriveUserAuthKey(
                salt = loginDetails.loginSalt,
                password = password,
                alg = loginDetails.loginAlg
            )
            val session: UserSession
            val user: UserMeRaw

            try {
                logg.d("login", "Start login to remote")
                delay(500)

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
                delay(500)
                val keys = userKeyRepository.getRemoteKeys()

                logg.d("login", "Found ${keys.size} key(s) on remote")

                val correctPersonalKey = keys.reversed().firstNotNullOfOrNull {
                    try {
                        // try getting correct keys from encrypted data that matches password
                        val personalKeyData = cryptoCore.decryptUserPersonalKey(
                            masterAuthKey = authKey.masterAuthKey,
                            userId = user.id,
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
                        logg.w("login", "Error parsing key from remote", e)
                        null
                    }
                }

                logg.d("login", "Found correct personal key: ${correctPersonalKey != null}")
                if (correctPersonalKey == null) {
                    // data is corrupted, current user password does not match user key
                    // this will trigger account recovery
                    config.temporaryMasterAuthKey = BaseRemoteModule.formatter.encodeToString(authKey)
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
                                userKeyRepository.uploadNewUserKey(
                                    keyRaw = correctPersonalKey.encryptedKeyData,
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
                config.temporaryMasterAuthKey = BaseRemoteModule.formatter.encodeToString(authKey)
                userRepo.initLocallyAfterSignUp(user, session)
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            // at this point we have both user and user key

            logg.d("login", "Start initializing user locally")
            emit(SignUpStep.INIT_USER_DATA)
            try {
                delay(500)
                logg.d("login", "Save user keys locally")
                userKeyRepository.saveUserKeyLocally(
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
        }.catch {
            logg.e("login", "Login not successful", it)
            throw it
        }
    }
}