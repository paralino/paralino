package app.paralino.core.repo.api

import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserSession

interface IUserApi {
    suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw
    suspend fun login(email: String, password: String): UserSession
    suspend fun getMe(): UserMeRaw
    suspend fun getCurrentSession(): UserSession
    suspend fun getAllSessions(): List<UserSession>
    suspend fun updateName(name: String): UserMeRaw
    suspend fun updateUserData(data: Map<String, Any>): UserMeRaw
    suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw
    suspend fun initResetPassword(email: String)
    suspend fun resetPassword(userId: String, secret: String, password: String)
    suspend fun initEmailVerification()
    suspend fun confirmEmail(userId: String, secret: String)
    suspend fun updateEmail(email: String, password: String): UserMeRaw
    suspend fun deleteAccount()
    suspend fun logout()
}