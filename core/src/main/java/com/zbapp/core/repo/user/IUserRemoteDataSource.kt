package com.zbapp.core.repo.user

import com.zbapp.core.models.user.UserMe

interface IUserRemoteDataSource {

    suspend fun getMe(): UserMe

    suspend fun updateName(name: String)
    suspend fun updateImage(imageBase64: String)
    suspend fun changePassword(newPassword: String)

    suspend fun generateUsername(): String
    suspend fun signUp(username: String, email: String, password: String): String
    suspend fun login(email: String, password: String): String
    suspend fun logout()
    suspend fun restoreSession(accountData: String)

}