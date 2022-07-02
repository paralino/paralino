package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.mappers.IDataMapperLocal
import com.zbapp.core.models.mappers.UserMapperCore
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.locationtrack.data.models.user.UserEntity

class UserMapperLocal constructor(
    val coreMapper: UserMapperCore,
) : IDataMapperLocal<UserEntity, UserMeRaw> {

    suspend fun local2Core(model: UserEntity): UserMe {
        return coreMapper.remote2Core(local2Remote(model))
    }

    override suspend fun local2Remote(model: UserEntity): UserMeRaw {
        return UserMeRaw(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = model.data
        )
    }

    override suspend fun remote2Local(model: UserMeRaw): UserEntity {
        return UserEntity(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = model.data
        )
    }
}

