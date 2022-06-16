package com.zbapp.core.domain.user

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
import kotlinx.coroutines.flow.first
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
            val user: Pair<UserMeRaw, UserSession>
            try {
                delay(500)
                // encrypted name can not fit in database field in appwrite
                // user name is saved encrypted to user preferences
                user = userRepo.remoteSignUp(userName = null, email, derivedPasswords.loginPassword)
                    .first()
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.STARTING, e)
            }

            // user is created at server and have a valid session

            // TODO clear remote cookies in case of error down the line

            emit(SignUpStep.GENERATING_KEYS)
            try {
                delay(500)
                val keys = prepareUserKeys(derivedPasswords.backupPassword, user.first.id)
                val remoteKey = userKeyRepository.uploadNewUserKey(keys.encryptedKeyData)
                userKeyRepository.saveUserKeyLocally(remoteKey, keys.userPersonalKey)
            } catch (e: Exception) {
                userKeyRepository.clearAll()
                throw SignUpStepException(SignUpStep.GENERATING_KEYS, e)
            }

            // user and user keys are ready

            emit(SignUpStep.INIT_USER_DATA)
            val updatedUser: UserMeRaw? = try {
                delay(500)
                val userData = UserMeData(userImage = userImage, userName = userName)
                val remoteData = userMapper.core2Remote(userData)
                userRepo.uploadAndInitKeysAndData(remoteData).first()
            } catch (e: Exception) {
                // ignore exception here
                // if user data (name and photo) are missing, show screen to complete the profile after
//                throw SignUpStepException(SignUpStep.INIT_USER_DATA, e)
                null
            }

            emit(SignUpStep.SAVE_USER)
            try {
                delay(500)
                userRepo.initLocallyAfterSignUp(updatedUser ?: user.first, user.second)
            } catch (e: Exception) {
                throw SignUpStepException(SignUpStep.SAVE_USER, e)
            }

            emit(SignUpStep.ALL_DONE)
        }
            .catch {
                logg.e(it)
                throw it
            }
    }

    private suspend fun prepareUserKeys(password: String, userId: String): GeneratedUserKeys {
        val keys = cryptoCore.generateNewKeysForUser(
            backupPassword = password,
            userId = userId,
            keyVersion = nowUTC().epochSeconds
        )
        return keys
    }

}