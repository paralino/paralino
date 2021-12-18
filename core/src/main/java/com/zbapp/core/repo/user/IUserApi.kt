package com.zbapp.core.repo.user

import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserSession
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.json.*

interface IUserApi {
    suspend fun signUp(userName: String?, email: String, password: String): UserResponse
    suspend fun login(email: String, password: String): SessionResponse
    suspend fun createAnonymous(): SessionResponse
    suspend fun getMe(): UserResponse
    suspend fun updateName(name: String): UserResponse
    suspend fun updateUserData(data: Map<String, Any>): UserResponse
    suspend fun changePassword(newPassword: String, oldPassword: String?): UserResponse
    suspend fun updateEmail(email: String, password: String): UserResponse
    suspend fun deleteAccount()
    suspend fun logout()

    companion object {
        fun getUserFromJson(content: String): UserMeRaw {
            val json = BaseRemoteModule.formatter.parseToJsonElement(content)
            return UserMeRaw(
                id = json.jsonObject["\$id"]?.jsonPrimitive?.content!!,
                name = json.jsonObject["name"]?.jsonPrimitive?.content ?: "",
                email = json.jsonObject["email"]?.jsonPrimitive?.content ?: "",
                registration = json.jsonObject["registration"]?.jsonPrimitive?.longOrNull ?: 0,
                emailVerification = json.jsonObject["emailVerification"]?.jsonPrimitive?.booleanOrNull ?: false,
                data = BaseRemoteModule.formatter.decodeFromJsonElement<UserMeDataRaw>(json.jsonObject["prefs"]!!.jsonObject)
            )
        }

        fun getSessionDataJson(content: String): UserSession {
            val json = BaseRemoteModule.formatter.parseToJsonElement(content)
            return UserSession(
                id = json.jsonObject["\$id"]?.jsonPrimitive?.content!!,
                expire = json.jsonObject["expire"]?.jsonPrimitive?.longOrNull ?: 0,
            )
        }
    }
}

typealias UserResponse = String
typealias SessionResponse = String