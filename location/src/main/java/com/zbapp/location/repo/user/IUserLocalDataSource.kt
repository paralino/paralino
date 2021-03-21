package com.zbapp.location.repo.user

import com.zbapp.location.models.user.UserMe
import kotlinx.coroutines.flow.Flow

interface IUserLocalDataSource {

    fun getMe(): Flow<UserMe>
    suspend fun getMeOnce(): UserMe?
    suspend fun update(data: UserMe)
    suspend fun clear()

}