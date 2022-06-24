package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.models.base.asImageModel
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData

class UserMapperCore(
    private val cryptoCore: CryptoCore,
    private val userKeyRepo: CryptoUserKeyRepository
) {

    suspend fun remote2Core(model: UserMeRaw): UserMe {
        return UserMe(
            id = model.id,
            email = model.email,
            registration = model.registration,
            emailVerification = model.emailVerification,
            data = model.data?.let { remote2Core(it) }
        )
    }

    suspend fun remote2Core(model: UserMeDataRaw): UserMeData {
        val userKey = userKeyRepo.personalKey.content
        return UserMeData(
            userImage = model.userImage?.let { cryptoCore.decryptDataWithKey(it, userKey) }?.asImageModel(),
            userName = model.userName?.let { cryptoCore.decryptDataWithKey(it, userKey) } ?: ""
        )
    }

    suspend fun core2Remote(model: UserMeData): UserMeDataRaw {
        val userKey = userKeyRepo.personalKey.content
        return UserMeDataRaw(
            userImage = model.userImage?.image?.let { cryptoCore.encryptDataWithKey(it, userKey) },
            userName = model.userName.takeIf { it.isNotBlank() }?.let { cryptoCore.encryptDataWithKey(it, userKey) }
        )
    }

}