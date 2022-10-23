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
import app.paralino.core.utils.exceptions.MissingUserKeyException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class LoginUseCase constructor(
    val userRepo: UserRepository,
    val cryptoCore: CryptoCore,
    val userKeyRepository: CryptoUserKeyRepository,
    val keyMapper: UserKeyMapperCore,
    val logoutUseCase: LogoutUseCase,
) {

    fun login(email: String, password: String): Flow<SignUpStep> {
        return flow {

            emit(SignUpStep.STARTING)
            val derivedPasswords = cryptoCore.derivePassword(password)
            val session: UserSession
            val user: UserMeRaw

            try {
                delay(500)
                session = userRepo.login(email, derivedPasswords.loginPassword)
                user = userRepo.getUserFromRemote()
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.STARTING, e)
            }

            // user and session are ready
            // login details are correct at this point

            emit(SignUpStep.GENERATING_KEYS)
            val keyData: GeneratedUserKeys
            try {
                delay(500)
                val keys = userKeyRepository.getRemoteKeys()

                val correctPersonalKey = keys.reversed().firstNotNullOfOrNull {
                    try {
                        // try getting correct keys from encrypted data that matches password
                        val personalKeyData = cryptoCore.signIn(
                            password = derivedPasswords.backupPassword,
                            userId = user.id,
                            keyData = it.encryptedSymmetricKey
                        )

                        // check if keypair is valid by decrypting
                        keyMapper.mapKeyPair(
                            model = it,
                            userKey = personalKeyData.asBase64String(cryptoCore.hashProvider)
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

                if (correctPersonalKey == null) {
                    // data is corrupted, current user password does not match user key
                    // this will trigger account recovery
                    config.temporaryBackupPassword = derivedPasswords.backupPassword
                    userRepo.initLocallyAfterSignUp(user, session)
                    throw MissingUserKeyException(false)
                } else {
                    keys.filter { it.id != correctPersonalKey.encryptedKeyData.id }
                        .takeIf { it.isNotEmpty() }
                        ?.let {
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
                config.temporaryBackupPassword = derivedPasswords.backupPassword
                userRepo.initLocallyAfterSignUp(user, session)
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            // at this point we have both user and user key

            emit(SignUpStep.INIT_USER_DATA)
            try {
                delay(500)
                userKeyRepository.saveUserKeyLocally(
                    keyData.encryptedKeyData,
                    keyData.userPersonalKey
                )
                userRepo.initLocallyAfterSignUp(user, session)
            } catch (e: Exception) {
                logoutUseCase.logoutLocally()
                throw SignUpStepException(SignUpStep.INIT_USER_DATA, e)
            }

            emit(SignUpStep.ALL_DONE)
        }.catch {
            logg.e(it)
            throw it
        }
    }

}