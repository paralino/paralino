package com.zbapp.core.repo.user

import com.zbapp.core.models.user.UserMe

interface IUserRemoteDataSource {

    suspend fun getMe(): UserMe
    suspend fun updateName(name: String)
    suspend fun updateImage(imageBase64: String)
    suspend fun changePassword(newPassword: String)

}