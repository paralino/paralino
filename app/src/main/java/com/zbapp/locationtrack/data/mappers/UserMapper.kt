package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.base.IUserCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.locationtrack.data.models.user.UserEntity

class UserMapper(
    private val crypto: IUserCrypto
) : IUserDataMapperCore, IDataMapperLocal<UserEntity, UserMeRaw> {
    
    override suspend fun local2Remote(model: UserEntity): UserMeRaw {
        return crypto.decryptModel<UserMeRaw>(model.data)?.copy(id = model.id)
            ?: UserMeRaw(id = model.id)
    }

    override suspend fun remote2Local(model: UserMeRaw): UserEntity {
        return UserEntity(
            id = model.id,
            data = crypto.encryptModel(model)
        )
    }

    override suspend fun remote2Core(model: UserMeRaw): UserMe {
        return UserMe(
            id = model.id,
            name = model.name,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = crypto.decryptModel(model.data)
        )
    }

    override suspend fun core2Remote(model: UserMe): UserMeRaw {
        return UserMeRaw(
            id = model.id,
            name = model.name,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = crypto.encryptModel(model.data)
        )
    }
}