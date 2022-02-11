package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class UserDataMapper(
    private val cryptoCore: CryptoCore
) : IDataMapperCore<UserMeData, UserMeDataRaw> {
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