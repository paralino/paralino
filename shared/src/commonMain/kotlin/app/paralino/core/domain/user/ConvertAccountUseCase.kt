package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.repo.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class ConvertAccountUseCase constructor(
    private val userRepo: UserRepository,
    private val changeCredentials: ChangeCredentialsUseCase,
    private val appPrefs: AppPrefs
): LimitedExecution() {
    fun convertAccount(
        email: String,
        password: String,
    ): Flow<ChangeCredentialsStep> {
        return changeCredentials.changeCredentials(
            userId = appPrefs.userId,
            oldPassword = appPrefs.guestPassword,
            newPassword = password,
            action = { oldLoginKey, newLoginKey ->
                userRepo.changePassword(
                    oldPassword = oldLoginKey!!,
                    newPassword = newLoginKey
                )

                appPrefs.guestPassword = password

                userRepo.updateUserEmail(
                    newEmail = email,
                    password = newLoginKey
                )
            }
        ).onEach {
            if (it == ChangeCredentialsStep.DONE) {
                appPrefs.guestEmail = ""
                appPrefs.guestPassword = ""
            }
        }
            .startProcessing("convert_account")
    }
}