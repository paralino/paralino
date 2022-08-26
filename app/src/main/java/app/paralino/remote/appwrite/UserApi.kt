package app.paralino.remote.appwrite

import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserSession
import app.paralino.core.repo.api.IUserApi
import io.appwrite.models.Session
import io.appwrite.models.User

class UserApi constructor(
    private val remoteModule: AppWriteRemoteModule
) : IUserApi {

    override suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return remoteModule.execute {
            account.create(uniqueIdPlaceholder, email, password, userName).parseUser()
        }
    }

    override suspend fun login(email: String, password: String): UserSession {
        return remoteModule.execute {
            account.createEmailSession(email, password).parseSession()
        }
    }

    override suspend fun createAnonymous(): UserSession {
        return remoteModule.execute { account.createAnonymousSession().parseSession() }
    }

    override suspend fun getMe(): UserMeRaw {
        return remoteModule.execute { account.get().parseUser() }
    }

    override suspend fun updateName(name: String): UserMeRaw {
        return remoteModule.execute { account.updateName(name).parseUser() }
    }

    override suspend fun updateUserData(data: Map<String, Any>): UserMeRaw {
        return remoteModule.execute { account.updatePrefs(data).parseUser() }
    }

    override suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw {
        return remoteModule.execute {
            account.updatePassword(newPassword, oldPassword).parseUser()
        }
    }

    override suspend fun initResetPassword(email: String) {
        remoteModule.execute {
            account.createRecovery(email, remoteModule.config.serverUrl)
        }
    }

    override suspend fun resetPassword(userId: String, secret: String, password: String) {
        return remoteModule.execute {
            account.updateRecovery(userId = userId, secret = secret, password = password, passwordAgain = password)
        }
    }

    override suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return remoteModule.execute { account.updateEmail(email, password).parseUser() }
    }

    override suspend fun deleteAccount() {
        return remoteModule.execute { account.updateStatus() }
    }

    override suspend fun logout() {
        return remoteModule.execute { account.deleteSessions() }
    }

    private fun User.parseUser(): UserMeRaw {
        return UserMeRaw(
            id = this.id,
            email = this.email,
            registration = this.registration,
            emailVerification = this.emailVerification,
            data = UserMeDataRaw.parseData(this.prefs.data)
        )
    }

    private fun Session.parseSession(): UserSession {
        return UserSession(
            id = this.id,
            expire = this.expire,
        )
    }

}