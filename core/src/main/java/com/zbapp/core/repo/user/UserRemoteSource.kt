package com.zbapp.core.repo.user

import com.zbapp.core.models.user.UserMe
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.core.utils.blocking
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UserRemoteSource constructor(
    private val api: IUserApi
) {

    suspend fun signUp(userName: String, email: String, password: String): UserMe {
        return executeUserOperation { api.signUp(userName, email, password) }
    }

    suspend fun login(email: String, password: String): UserMe {
        return blocking {
            api.login(email, password)
            getUserFromJson(api.getMe())
        }
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

    suspend fun updateUserData(data: Map<String, String>): UserMe {
        return executeUserOperation { api.updateUserData(data) }
    }

    suspend fun logout() {
        blocking { api.logout() }
    }

    private suspend fun executeUserOperation(action: suspend () -> String): UserMe {
        return blocking { getUserFromJson(action()) }
    }

    private fun getUserFromJson(content: String): UserMe {
        val json = BaseRemoteModule.formatter.parseToJsonElement(content)
        val user = BaseRemoteModule.formatter.decodeFromJsonElement<UserMe>(json)
        user.imageBase64 = json.jsonObject["prefs"]?.jsonObject?.get("userImage")?.jsonPrimitive?.content
        return user
    }

}