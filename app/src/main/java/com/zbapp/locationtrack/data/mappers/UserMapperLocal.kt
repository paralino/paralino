package com.zbapp.locationtrack.data.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.mappers.IDataMapperLocal
import com.zbapp.core.models.mappers.UserMapperCore
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.locationtrack.data.models.user.UserEntity

class UserMapperLocal(
    val coreMapper: UserMapperCore,
    private val cryptoCore: CryptoCore
) : IDataMapperLocal<UserEntity, UserMeRaw> {

    suspend fun local2Core(model: UserEntity): UserMe {
        return coreMapper.remote2Core(local2Remote(model))
    }

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
}

