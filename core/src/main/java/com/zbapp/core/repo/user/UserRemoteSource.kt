package com.zbapp.core.repo.user

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserSession
import kotlinx.serialization.json.JsonObject

class UserRemoteSource constructor(
    private val api: IUserApi
) {

    suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw {
        return executeUserOperation { api.signUp(userName, email, password) }
    }

    suspend fun signUpAndCreateSession(userName: String?, email: String, password: String): Pair<UserMeRaw, UserSession> {
        return signUp(userName, email, password).let {
            it to login(email, password)
        }
    }

    suspend fun login(email: String, password: String): UserSession {
        return executeSessionOperation { api.login(email, password) }
    }

    suspend fun singUpAnonymously(): UserSession {
        return executeSessionOperation { api.createAnonymous() }
    }

    suspend fun getMe(): UserMeRaw {
        return executeUserOperation { api.getMe() }
    }

    suspend fun updateName(name: String): UserMeRaw {
        return executeUserOperation { api.updateName(name) }
    }

    suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw {
        return executeUserOperation { api.changePassword(newPassword, oldPassword) }
    }

    suspend fun updateUserImage(imageBase64: String): UserMeRaw {
        return executeUserOperation { api.updateUserData(mapOf("imageBase64" to imageBase64)) }
    }

    suspend fun updateUserData(data: Map<String, Any>): UserMeRaw {
        return executeUserOperation { api.updateUserData(data) }
    }

    suspend fun updateUserData(data: JsonObject): UserMeRaw {
        return executeUserOperation { api.updateUserData(data) }
    }

    suspend fun updateEmail(email: String, password: String): UserMeRaw {
        return executeUserOperation { api.updateEmail(email, password) }
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
    }

    suspend fun logout() {
        api.logout()
    }

    private suspend fun executeUserOperation(action: suspend () -> String): UserMeRaw {
        return IUserApi.getUserFromJson(action())
    }

    private suspend fun executeSessionOperation(action: suspend () -> String): UserSession {
        return IUserApi.getSessionDataJson(action())
    }

}