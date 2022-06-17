package com.zbapp.core.domain.user

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.GeneratedUserKeys
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.logg
import com.zbapp.core.models.mappers.UserKeyMapperCore
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.presentation.login.SignUpStep
import com.zbapp.core.presentation.login.SignUpStepException
import com.zbapp.core.repo.user.UserRepository
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
) {

    fun login(email: String, password: String): Flow<SignUpStep> {
        return flow {

            emit(SignUpStep.STARTING)
            val derivedPasswords = cryptoCore.derivePassword(password)
            val session: UserSession
            val user: UserMeRaw

            try {
                delay(500)
                session = userRepo.login(email, derivedPasswords.loginPassword).first()
                user = userRepo.getUserIfAvailable().first()
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

                val correctPersonalKey = keys.firstNotNullOfOrNull {
                    try {
                        // try getting correct keys from encrypted data that matches password
                        val personalKeyData = cryptoCore.signIn(
                            password = derivedPasswords.backupPassword,
                            userId = user.id,
                            keyData = it.encryptedSymmetricKey
                        )

                        // check if keypair is valid by decrypting
                        keyMapper.mapKeyPair(
                            it,
                            personalKeyData.asBase64String(cryptoCore.hashProvider)
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
                    if (keys.isNotEmpty()) {
                        // TODO
                        // data is corrupted, current user password does not match user key
                        // user should either change password to old one to be able to decrypt data
                        // or reset profile (equivalent to forgot password)
                    }

                    val newKey = cryptoCore.generateNewKeysForUser(
                        backupPassword = derivedPasswords.backupPassword,
                        userId = user.id
                    )

                    val remoteKey = userKeyRepository.uploadNewUserKey(newKey.encryptedKeyData)
                    userKeyRepository.saveUserKeyLocally(remoteKey, newKey.userPersonalKey)

                    keyData = newKey
                } else {
                    keys.filter { it.id != correctPersonalKey.encryptedKeyData.id }.forEach {
                        try {
                            userKeyRepository.deleteKey(it.id).first()
                        } catch (e: Exception) {
                            // ignore error if key is not deleted
                        }
                    }
                    keyData = correctPersonalKey
                }

            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            emit(SignUpStep.INIT_USER_DATA)
            try {
                delay(500)
                userKeyRepository.saveUserKeyLocally(
                    keyData.encryptedKeyData,
                    keyData.userPersonalKey
                )
            } catch (e: Exception) {
                userKeyRepository.clearAll()
                throw SignUpStepException(SignUpStep.INIT_USER_DATA, e)
            }

            emit(SignUpStep.SAVE_USER)
            try {
                userRepo.initLocallyAfterSignUp(user, session)
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.SAVE_USER, e)
            }

            emit(SignUpStep.ALL_DONE)
        }.catch {
            logg.e(it)
            throw it
        }
    }

}