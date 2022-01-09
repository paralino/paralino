package com.zbapp.core.repo.user

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserSession

interface IUserApi {
    suspend fun signUp(userName: String?, email: String, password: String): UserMeRaw
    suspend fun login(email: String, password: String): UserSession
    suspend fun createAnonymous(): UserSession
    suspend fun getMe(): UserMeRaw
    suspend fun updateName(name: String): UserMeRaw
    suspend fun updateUserData(data: Map<String, Any>): UserMeRaw
    suspend fun changePassword(newPassword: String, oldPassword: String?): UserMeRaw
    suspend fun updateEmail(email: String, password: String): UserMeRaw
    suspend fun deleteAccount()
    suspend fun logout()
}