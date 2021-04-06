package com.zbapp.core.repo.user

import com.zbapp.core.models.user.UserMe

interface IUserRemoteDataSource {

    suspend fun getMe(): UserMe
    suspend fun update(data: UserMe)

}