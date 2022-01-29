package com.zbapp.core.repo.user

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import kotlinx.coroutines.flow.Flow

interface IUserLocalDataSource {

    fun getMe(): Flow<UserMeRaw>
    suspend fun update(data: UserMeRaw): UserMe
    suspend fun clear()

}