package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.user.UserMe
import com.zbapp.locationtrack.data.models.user.UserEntity

object UserMapper: IDataMapperLocal<UserMe, UserEntity> {
    override fun local2Core(model: UserEntity): UserMe {
        return UserMe(
            id = model.id,
            name = model.name,
            email = model.email,
            imageBase64 = model.imageBase64
        )
    }

    override fun core2Local(model: UserMe): UserEntity {
        return UserEntity(
            id = model.id,
            name = model.name,
            email = model.email,
            imageBase64 = model.imageBase64
        )
    }
}