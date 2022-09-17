package app.paralino.core.domain.user

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.repo.user.UserRepository
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