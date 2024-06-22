package app.paralino.core.domain.user

import app.paralino.core.config
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class ConvertAccountUseCase constructor(
    private val userRepo: UserRepository,
    private val changeCredentials: ChangeCredentialsUseCase,
) {
    fun convertAccount(
        email: String,
        password: String,
    ): Flow<ChangeCredentialsStep> {
        return changeCredentials.changeCredentials(
            userId = config.userId,
            newSalt = email,
            oldPassword = config.guestPassword,
            newPassword = password,
            action = { oldLoginKey, newLoginKey ->
                userRepo.changePassword(
                    oldPassword = oldLoginKey,
                    newPassword = newLoginKey
                )

                config.guestPassword = password

                userRepo.updateUserEmail(
                    newEmail = email,
                    password = newLoginKey
                )
            }
        ).onEach {
            if (it == ChangeCredentialsStep.DONE) {
                config.guestEmail = ""
                config.guestPassword = ""
            }
        }
    }
}