package com.zbapp.core.domain.user

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow

class ChangeEmailUseCase(
    private val userRepository: UserRepository,
    private val cryptoCore: CryptoCore
) {

    fun changeEmail(email: String, password: String): Flow<Unit> {
        val derivedPassword = cryptoCore.derivePassword(password)
        return userRepository.updateUserEmail(email, derivedPassword.loginPassword)
    }

}