package com.zbapp.core.domain.user

import com.zbapp.core.config
import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.GeneratedUserKeys
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.logg
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.models.mappers.UserMapperCore
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.presentation.login.SignUpStep
import com.zbapp.core.presentation.login.SignUpStepException
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.extensions.nowUTC
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class CreateAccountUseCase constructor(
    val userRepo: UserRepository,
    val cryptoCore: CryptoCore,
    val userKeyRepository: CryptoUserKeyRepository,
    val userMapper: UserMapperCore,
) {

    fun signUp(
        userName: String,
        userImage: ImageModel,
        email: String,
        password: String,
    ): Flow<SignUpStep> {
        return flow {

            emit(SignUpStep.STARTING)
            val derivedPasswords = cryptoCore.derivePassword(password)
            val user: UserMeRaw
            val session: UserSession

            try {
                delay(500)
                val response = userRepo.remoteSignUp(email, derivedPasswords.loginPassword)
                user = response.first
                session = response.second
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.STARTING, e)
            }

            // user is created at server and have a valid session

            emit(SignUpStep.GENERATING_KEYS)
            try {
                delay(500)
                initAndSaveUserKeys(derivedPasswords.backupPassword, user.id)
            } catch (e: Exception) {
                userKeyRepository.clearAll()
                // key generation failed but user is created
                // we are saving user as is, so account recovery can take place later on
                config.temporaryBackupPassword = derivedPasswords.backupPassword
                userRepo.initLocallyAfterSignUp(user, session)
                throw e
            }

            // user and user keys are ready

            emit(SignUpStep.INIT_USER_DATA)
            try {
                delay(500)
                val userData = UserMeData(userImage = userImage, userName = userName)
                val remoteData = userMapper.core2Remote(userData)
                userRepo.uploadAndInitKeysAndData(remoteData).also {
                    delay(500)
                    userRepo.initLocallyAfterSignUp(it, session)
                }
            } catch (e: Exception) {
                // ignore exception here
                // if user data (name and photo) are missing, show screen to complete the profile after
                userRepo.initLocallyAfterSignUp(user, session)
            }

            emit(SignUpStep.ALL_DONE)
        }
            .catch {
                logg.e(it)
                throw it
            }
    }

    suspend fun initAndSaveUserKeys(backupPassword: String, userId: String) {
        try {
            val keys = createNewKey(backupPassword, userId)
            val remoteKey = userKeyRepository.uploadNewUserKey(keys.encryptedKeyData)
            userKeyRepository.saveUserKeyLocally(remoteKey, keys.userPersonalKey)
        } catch (e: Exception) {
            userKeyRepository.clearAll()
            throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
        }
    }

    suspend fun createNewKey(backupPassword: String, userId: String): GeneratedUserKeys {
        return cryptoCore.generateNewKeysForUser(
            backupPassword = backupPassword,
            userId = userId,
            keyVersion = nowUTC().epochSeconds
        )
    }

}