package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.mappers.IUserDataMapperCore
import com.zbapp.core.models.mappers.IUserMapperCore
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.utils.BaseRemoteModule
import com.zbapp.locationtrack.data.models.user.UserEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class UserMapper(
    private val cryptoCore: CryptoCore,
    val userDataMapper: IUserDataMapperCore
) : IUserMapperCore, IDataMapperLocal<UserEntity, UserMeRaw> {

    override suspend fun local2Remote(model: UserEntity): UserMeRaw {
        return model.data?.let {
            cryptoCore.decryptModel<UserMeRaw>(
                data = it,
                key = cryptoCore.loadUserPersonalKey().content
            )?.copy(id = model.id)
        }
            ?: UserMeRaw(id = model.id)
    }

    override suspend fun remote2Local(model: UserMeRaw): UserEntity {
        return UserEntity(
            id = model.id,
            data = cryptoCore.encryptModel(model, cryptoCore.loadUserPersonalKey().content)
        )
    }

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

class UserDataMapper(
    private val cryptoCore: CryptoCore
) : IUserDataMapperCore {
    override suspend fun remote2Core(model: UserMeDataRaw): UserMeData {
        val userKey = cryptoCore.loadUserPersonalKey().content
        return UserMeData(
            imageBase64 = model.imageBase64?.let { cryptoCore.decryptDataWithKey(it, userKey) },
            userName = model.userName?.let { cryptoCore.decryptDataWithKey(it, userKey) } ?: "",
            privateKeyBackup = model.privateKeyBackup?.let {
                BaseRemoteModule.formatter.decodeFromString(it)
            },
            asymmetricKeys = model.asymmetricKeys?.map { cryptoCore.decryptModel(it, userKey)!! }
        )
    }

    override suspend fun core2Remote(model: UserMeData): UserMeDataRaw {
        val userKey = cryptoCore.loadUserPersonalKey().content
        return UserMeDataRaw(
            imageBase64 = model.imageBase64?.let { cryptoCore.encryptDataWithKey(it, userKey) },
            userName = model.userName.let { cryptoCore.encryptDataWithKey(it, userKey) },
            privateKeyBackup = model.privateKeyBackup?.let {
                BaseRemoteModule.formatter.encodeToString(it)
            },
            asymmetricKeys = model.asymmetricKeys?.map { cryptoCore.encryptModel(it, userKey)!! }
        )
    }

}