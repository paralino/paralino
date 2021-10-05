package com.zbapp.core.repo.user

import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UserRemoteSource constructor(
    private val api: IUserApi
) {

    suspend fun signUp(userName: String?, email: String, password: String): UserMe {
        return executeUserOperation { api.signUp(userName, email, password) }
    }

    suspend fun signUpAncCreateSession(userName: String?, email: String, password: String): Pair<UserMe, UserSession> {
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

    suspend fun getMe(): UserMe {
        return executeUserOperation { api.getMe() }
    }

    suspend fun updateName(name: String): UserMe {
        return executeUserOperation { api.updateName(name) }
    }

    suspend fun changePassword(newPassword: String, oldPassword: String?): UserMe {
        return executeUserOperation { api.changePassword(newPassword, oldPassword) }
    }

    suspend fun updateUserImage(imageBase64: String): UserMe {
        return executeUserOperation { api.updateUserData(mapOf("userImage" to imageBase64)) }
    }

    suspend fun updateEmail(email: String, password: String): UserMe {
        return executeUserOperation { api.updateEmail(email, password) }
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
    }

    suspend fun logout() {
        api.logout()
    }

    private suspend fun executeUserOperation(action: suspend () -> String): UserMe {
        return IUserApi.getUserFromJson(action())
    }

    private suspend fun executeSessionOperation(action: suspend () -> String): UserSession {
        return IUserApi.getSessionDataJson(action())
    }

}