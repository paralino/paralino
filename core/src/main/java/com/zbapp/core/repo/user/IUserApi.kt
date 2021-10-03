package com.zbapp.core.repo.user

interface IUserApi {
    suspend fun signUp(userName: String?, email: String, password: String): UserResponse
    suspend fun login(email: String, password: String): SessionResponse
    suspend fun getMe(): UserResponse
    suspend fun updateName(name: String): UserResponse
    suspend fun updateUserData(data: Map<String, String>): UserResponse
    suspend fun changePassword(newPassword: String, oldPassword: String?): UserResponse
    suspend fun logout()
}

typealias UserResponse = String
typealias SessionResponse = String