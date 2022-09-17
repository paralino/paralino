package app.paralino.core.domain.user

import app.paralino.core.config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

class ConvertAccountUseCase constructor(
    private val changeEmail: ChangeEmailUseCase,
    private val changePassword: ChangePasswordUseCase,
) {

    fun convertAccount(
        email: String,
        password: String,
    ): Flow<Unit> {
        val oldPassword = config.guestPassword
        return changePassword.changePassword(oldPassword = oldPassword, newPassword = password)
            .flatMapLatest {
                changeEmail.changeEmail(email = email, password = password)
                    .onEach {
                        config.guestEmail = ""
                        config.guestPassword = ""
                    }
            }

    }

}