package com.zbapp.core.repo.user

interface IUserApi {
    suspend fun signUp(userName: String, email: String, password: String): String
    suspend fun login(email: String, password: String): String
    suspend fun getMe(): String
    suspend fun updateName(name: String): String
    suspend fun updateUserData(data: Map<String, String>): String
    suspend fun changePassword(newPassword: String, oldPassword: String?): String
    suspend fun logout()
}