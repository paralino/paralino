package app.paralino.core.domain.user

import app.paralino.core.domain.crypto.user.CryptoAccountOperationV2
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.models.user.SignUpStep
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.exceptions.NotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ResetPasswordUseCase(
    private val userRepository: UserRepository,
    private val cryptoAccountOperationV2: CryptoAccountOperationV2,
): LimitedExecution() {

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
            .startProcessing("reset_password_init")
    }

    fun resetPassword(
        email: String,
        userId: String,
        secret: String,
        password: String
    ): Flow<SignUpStep> {
        return flow {
            emit(SignUpStep.STARTING)
            val loginDetails = userRepository.getLoginDetails(key = userId)
            val authKey = cryptoAccountOperationV2.deriveUserAuthKey(
                email = email,
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
        }.startProcessing("reset_password")
    }
}