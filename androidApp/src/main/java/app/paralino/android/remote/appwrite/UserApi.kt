package app.paralino.android.remote.appwrite

import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserSession
import app.paralino.core.repo.api.IUserApi
import io.appwrite.models.User
import io.appwrite.models.Session

class UserApi constructor(
    private val remoteModule: AppWriteRemoteModule,
) : IUserApi {

    override suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return remoteModule.execute {
            account.create(this.appConfig.resourceIds.uniqueIdPlaceholder, email, password, userName).parseUser()
        }
    }

    override suspend fun login(email: String, password: String): UserSession {
        return remoteModule.execute {
            account.createEmailPasswordSession(email, password).parseSession()
        }
    }

    override suspend fun getCurrentSession(): UserSession {
        return remoteModule.execute { account.getSession("current").parseSession() }
    }

    override suspend fun renewCurrentSession(): UserSession {
        return remoteModule.execute { account.updateSession("current").parseSession() }
    }

    override suspend fun getAllSessions(): List<UserSession> {
        return remoteModule.execute { account.listSessions().sessions.map { it.parseSession() } }
    }

    override suspend fun getMe(): UserMeRaw {
        return remoteModule.execute(forceLogoutOnError = true) {
            account.get().parseUser()
        }
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
            account.createRecovery(email, remoteModule.appConfig.resourceIds.passwordResetUrl)
        }
    }

    override suspend fun resetPassword(userId: String, secret: String, password: String) {
        return remoteModule.execute {
            account.updateRecovery(
                userId = userId,
                secret = secret,
                password = password
            )
        }
    }

    override suspend fun initEmailVerification() {
        return remoteModule.execute {
            account.createVerification(remoteModule.appConfig.resourceIds.emailConfirmationUrl)
        }
    }

    override suspend fun confirmEmail(userId: String, secret: String) {
        return remoteModule.execute {
            account.updateVerification(userId = userId, secret = secret)
        }
    }

    override suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return remoteModule.execute { account.updateEmail(email, password).parseUser() }
    }

    override suspend fun deleteAccount() {
        return remoteModule.execute { account.updateStatus() }
    }

    override suspend fun logoutSession(sessionId: String) {
        return remoteModule.execute { account.deleteSession(sessionId = sessionId) }
    }

    override suspend fun logoutCurrentSession() {
        return remoteModule.execute { account.deleteSession(sessionId = "current") }
    }

    private fun User<Map<String, Any>>.parseUser(): UserMeRaw {
        return UserMeRaw(
            id = this.id,
            email = this.email,
            registration = this.registration,
            emailVerification = this.emailVerification,
            passwordUpdate = this.passwordUpdate,
            data = UserMeDataRaw.parseData(this.prefs.data)
        )
    }

    private fun Session.parseSession(): UserSession {
        return UserSession(
            id = this.id,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            expire = this.expire,
            provider = this.provider,
            ip = this.ip,
            osCode = this.osCode,
            osName = this.osName,
            osVersion = this.osVersion,
            clientType = this.clientType,
            clientCode = this.clientCode,
            clientName = this.clientName,
            clientVersion = this.clientVersion,
            clientEngine = this.clientEngine,
            clientEngineVersion = this.clientEngineVersion,
            deviceName = this.deviceName,
            deviceBrand = this.deviceBrand,
            deviceModel = this.deviceModel,
            countryCode = this.countryCode,
            countryName = this.countryName,
            current = this.current
        )
    }
}