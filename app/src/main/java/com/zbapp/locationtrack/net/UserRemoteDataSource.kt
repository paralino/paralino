package com.zbapp.locationtrack.net

import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserRemoteDataSource

class UserRemoteDataSource: IUserRemoteDataSource {

    override suspend fun getMe(): UserMe {
        return UserMe("user_me", "USER ME", "user@example.com", null, "2021-02-04")
    }

    override suspend fun update(data: UserMe) {
        // do update
    }

}