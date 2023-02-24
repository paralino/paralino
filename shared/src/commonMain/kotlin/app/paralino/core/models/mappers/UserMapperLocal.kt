package app.paralino.core.models.mappers

import app.paralino.core.data.db.UserEntity
import app.paralino.core.models.raw.UserMeDataRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe

class UserMapperLocal constructor(
    val coreMapper: UserMapperCore,
) {

    suspend fun local2Core(model: UserEntity): UserMe {
        return coreMapper.remote2Core(local2Remote(model))
    }

    fun local2Remote(model: UserEntity): UserMeRaw {
        return UserMeRaw(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = UserMeDataRaw(
                userImage = model.user_data_userImage,
                userName = model.user_data_userName
            )
        )
    }

    fun remote2Local(model: UserMeRaw): UserEntity {
        return UserEntity(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            user_data_userImage = model.data?.userImage,
            user_data_userName = model.data?.userName
        )
    }
}

