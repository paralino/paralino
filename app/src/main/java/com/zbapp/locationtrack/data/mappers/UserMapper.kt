package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.mappers.IUserMapperCore
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.repo.base.IUserCrypto
import com.zbapp.core.repo.base.decryptModel
import com.zbapp.core.repo.base.encryptModel
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.locationtrack.data.models.user.UserEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class UserMapper(
    private val crypto: IUserCrypto,
    val userDataMapper: IUserDataMapperCore
) : IUserMapperCore, IDataMapperLocal<UserEntity, UserMeRaw> {

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
            data = model.data?.let { userDataMapper.remote2Core(it) }
        )
    }

    override suspend fun core2Remote(model: UserMe): UserMeRaw {
        return UserMeRaw(
            id = model.id,
            name = model.name,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = model.data?.let { userDataMapper.core2Remote(it) }
        )
    }
}

class UserDataMapper(private val crypto: IUserCrypto) : IUserDataMapperCore {
    override suspend fun remote2Core(model: UserMeDataRaw): UserMeData {
        return UserMeData(
            imageBase64 = model.imageBase64?.let { crypto.decryptData(it) },
            privateKeyBackup = model.privateKeyBackup?.let {
                BaseRemoteModule.formatter.decodeFromString(it)
            },
            asymmetricKeys = model.asymmetricKeys?.map { crypto.decryptModel(it)!! }
        )
    }

    override suspend fun core2Remote(model: UserMeData): UserMeDataRaw {
        return UserMeDataRaw(
            imageBase64 = model.imageBase64?.let { crypto.encryptData(it) },
            privateKeyBackup = model.privateKeyBackup?.let {
                BaseRemoteModule.formatter.encodeToString(it)
            },
            asymmetricKeys = model.asymmetricKeys?.map { crypto.encryptModel(it)!! }
        )
    }

}