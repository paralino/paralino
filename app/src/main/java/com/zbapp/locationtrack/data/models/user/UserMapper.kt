package com.zbapp.locationtrack.data.models.user

import com.zbapp.location.models.user.UserMe
import com.zbapp.locationtrack.data.IDataMapper

object UserMapper: IDataMapper<UserEntity, UserMe> {
    override fun map(model: UserEntity): UserMe {
        return UserMe(
            id = model.id,
            name = model.name,
            email = model.email,
            imageBase64 = model.imageBase64,
            userSince = model.userSince
        )
    }

    override fun mapReversed(model: UserMe): UserEntity {
        return UserEntity(
            id = model.id,
            name = model.name,
            email = model.email,
            imageBase64 = model.imageBase64,
            userSince = model.userSince
        )
    }
}