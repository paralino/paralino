package com.zbapp.core.repo.user

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserSession

class UserRemoteSource constructor(
    private val api: IUserApi
) {

    suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return api.signUp(userName, email, password)
    }

    suspend fun signUpAndCreateSession(
        userName: String?,
        email: String,
        password: String
    ): Pair<UserMeRaw, UserSession> {
        return signUp(userName, email, password).let {
            it to login(email, password)
        }
    }

    suspend fun login(email: String, password: String): UserSession {
        return api.login(email, password)
    }

    suspend fun singUpAnonymously(): UserSession {
        return api.createAnonymous()
    }

    suspend fun getMe(): UserMeRaw {
        return api.getMe()
    }

    suspend fun updateName(name: String): UserMeRaw {
        return api.updateName(name)
    }

    suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw {
        return api.changePassword(newPassword, oldPassword)
    }

    suspend fun updateUserImage(imageBase64: String): UserMeRaw {
        return api.updateUserData(mapOf("imageBase64" to imageBase64))
    }

    suspend fun updateUserData(data: Map<String, Any>): UserMeRaw {
        return api.updateUserData(data)
    }

    suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return api.updateEmail(email, password)
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
    }

    suspend fun logout() {
        api.logout()
    }

}