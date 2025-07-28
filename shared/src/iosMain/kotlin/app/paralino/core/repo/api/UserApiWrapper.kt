package app.paralino.core.repo.api

import app.paralino.core.models.Payload
import app.paralino.core.models.getOrThrow
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserSession

class UserApiWrapperIOS(
    val userApi: IUserApiIOS
): IUserApi {
    override suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return userApi.signUp(userName, email, password).getOrThrow()
    }

    override suspend fun login(email: String, password: String): UserSession {
        return userApi.login(email, password).getOrThrow()
    }

    override suspend fun getMe(): UserMeRaw {
        return userApi.getMe().getOrThrow()
    }

    override suspend fun getCurrentSession(): UserSession {
        return userApi.getCurrentSession().getOrThrow()
    }

    override suspend fun getAllSessions(): List<UserSession> {
        return userApi.getAllSessions().getOrThrow()
    }

    override suspend fun updateUserData(data: Map<String, Any>): UserMeRaw {
        return userApi.updateUserData(data).getOrThrow()
    }

    override suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw {
        return userApi.changePassword(newPassword, oldPassword).getOrThrow()
    }

    override suspend fun initResetPassword(email: String) {
        userApi.initResetPassword(email).getOrThrow()
    }

    override suspend fun resetPassword(userId: String, secret: String, password: String) {
        userApi.resetPassword(userId, secret, password).getOrThrow()
    }

    override suspend fun initEmailVerification() {
        userApi.initEmailVerification().getOrThrow()
    }

    override suspend fun confirmEmail(userId: String, secret: String) {
        userApi.confirmEmail(userId, secret).getOrThrow()
    }

    override suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return userApi.updateEmail(email, password).getOrThrow()
    }

    override suspend fun deleteAccount() {
        userApi.deleteAccount().getOrThrow()
    }

    override suspend fun logoutSession(sessionId: String) {
        userApi.logoutSession(sessionId = sessionId).getOrThrow()
    }

    override suspend fun logoutCurrentSession() {
        userApi.logoutCurrentSession().getOrThrow()
    }
}

interface IUserApiIOS {
    suspend fun signUp(userName: String?, email: String, password: String): Payload<UserMeRaw>
    suspend fun login(email: String, password: String): Payload<UserSession>
    suspend fun getMe(): Payload<UserMeRaw>
    suspend fun getCurrentSession(): Payload<UserSession>
    suspend fun getAllSessions(): Payload<List<UserSession>>
    suspend fun updateUserData(data: Map<String, Any>): Payload<UserMeRaw>
    suspend fun changePassword(newPassword: String, oldPassword: String?): Payload<UserMeRaw>
    suspend fun initResetPassword(email: String): Payload<Unit>
    suspend fun resetPassword(userId: String, secret: String, password: String): Payload<Unit>
    suspend fun initEmailVerification(): Payload<Unit>
    suspend fun confirmEmail(userId: String, secret: String): Payload<Unit>
    suspend fun updateEmail(email: String, password: String): Payload<UserMeRaw>
    suspend fun deleteAccount(): Payload<Unit>
    suspend fun logoutSession(sessionId: String): Payload<Unit>
    suspend fun logoutCurrentSession(): Payload<Unit>
}