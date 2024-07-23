package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.presentation.login.SignUpStep
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.NotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.log

class ResetPasswordUseCase(
    private val userRepository: UserRepository,
    private val changeCredentials: ChangeCredentialsUseCase,
    private val cryptoCore: CryptoCore
) {

    fun initResetPassword(email: String): Flow<SignUpStep> {
        return flow {
            emit(SignUpStep.STARTING)
            try {
                userRepository.initResetPassword(email)
            } catch (e: Exception) {
                if (e is NotFoundException) {
                    // ignore
                } else {
                    throw e
                }
            }
            delay(500)
            emit(SignUpStep.ALL_DONE)
        }
    }

    fun resetPassword(userId: String, secret: String, password: String): Flow<SignUpStep> {
        return flow {
            emit(SignUpStep.STARTING)
            val loginDetails = userRepository.getLoginDetails(key = userId)
            val authKey = cryptoCore.deriveUserAuthKey(
                salt = loginDetails.loginSalt,
                password = password,
                alg = loginDetails.loginAlg
            )
            userRepository.resetPassword(
                userId = userId,
                secret = secret,
                password = authKey.loginKey
            )
            delay(500)
            emit(SignUpStep.ALL_DONE)
        }
    }
}