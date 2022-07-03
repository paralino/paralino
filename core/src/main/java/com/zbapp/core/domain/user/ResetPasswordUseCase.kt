package com.zbapp.core.domain.user

import com.zbapp.core.presentation.login.SignUpStep
import com.zbapp.core.repo.user.UserRepository
import com.zbapp.core.utils.exceptions.NotFoundException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ResetPasswordUseCase(
    private val userRepository: UserRepository,
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
            userRepository.resetPassword(userId, secret, password)
            delay(500)
            emit(SignUpStep.ALL_DONE)
        }
    }

}