package com.zbapp.location.repo.user

import com.zbapp.location.models.user.UserMe

interface IUserRemoteDataSource {

    suspend fun getMe(): UserMe
    suspend fun update(data: UserMe)

}