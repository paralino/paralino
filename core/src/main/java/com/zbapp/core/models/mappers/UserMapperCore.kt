package com.zbapp.core.models.mappers

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe

class UserMapperCore(
    private val userDataMapper: UserDataMapper
) : IDataMapperCore<UserMe, UserMeRaw> {

    override suspend fun remote2Core(model: UserMeRaw): UserMe {
        return UserMe(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = model.data?.let { userDataMapper.remote2Core(it) }
        )
    }

    override suspend fun core2Remote(model: UserMe): UserMeRaw {
        return UserMeRaw(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = model.data?.let { userDataMapper.core2Remote(it) }
        )
    }

}