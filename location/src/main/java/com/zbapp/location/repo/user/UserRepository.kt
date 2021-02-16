package com.zbapp.location.repo.user

import com.zbapp.location.models.Payload
import com.zbapp.location.models.user.UserMe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class UserRepository {

    fun getMe(): Flow<Result<UserMe>> {
        return flow {  }
    }

}