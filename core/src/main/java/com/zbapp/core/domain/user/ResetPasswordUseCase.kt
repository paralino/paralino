package com.zbapp.core.domain.user

import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ResetPasswordUseCase(
    private val userRepository: UserRepository,
) {

    fun initResetPassword(email: String): Flow<Unit> {
        return flow {
            userRepository.initResetPassword(email)
            emit(Unit)
        }
    }

    suspend fun resetPassword(userId: String, secret: String, password: String): Flow<Unit> {
        return flow {
            userRepository.resetPassword(userId, secret, password)
            emit(Unit)
        }
    }

}