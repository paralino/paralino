package app.paralino.core.domain.user

import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.LimitedExecution
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow

class ConvertAccountUseCase constructor(
    private val userRepo: UserRepository,
    private val changeCredentials: ChangeCredentialsUseCase,
    private val appPrefs: AppPrefs
): LimitedExecution() {
    fun convertAccount(
        email: String,
        password: String,
    ): Flow<ChangeCredentialsStep> {

        val (oldEmail, oldPassword) = if (appPrefs.hasV1GuestAccount()) {
            appPrefs.guestEmailV1 to appPrefs.guestPasswordV1
        } else if (appPrefs.hasV2GuestAccount()) {
            appPrefs.guestEmailV2 to appPrefs.guestPasswordV2
        } else {
            throw IllegalStateException("Missing guest account")
        }

        return changeCredentials.changeCredentials(
            userId = appPrefs.userId,
            oldEmail = oldEmail,
            newEmail = email,
            oldPassword = oldPassword,
            newPassword = password,
            replaceKey = true,
            action = { oldLoginKey, newLoginKey ->

                val currentUser = userRepo.refreshUserIfAvailable()
                val lastPasswordUpdate = currentUser.passwordUpdateTime
                val requestTime = nowUTC()

                try {
                    userRepo.updateUserEmailAndPassword(
                        newEmail = email,
                        oldPassword = oldLoginKey,
                        newPassword = newLoginKey
                    )
                } catch (e: Exception) {
                    val updatedUser = userRepo.refreshUserIfAvailable()
                    val isPasswordUpdated = updatedUser.passwordUpdateTime?.let {
                        it > (lastPasswordUpdate ?: requestTime)
                    } ?: false
                    if (updatedUser.email == email && isPasswordUpdated) {
                        // we can assume that data is updated
                    } else {
                        throw e
                    }
                }

                appPrefs.invalidateGuestAccount()
            }
        )
            .startProcessing("convert_account")
    }
}