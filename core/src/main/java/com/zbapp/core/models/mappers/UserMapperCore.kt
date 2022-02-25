package com.zbapp.core.models.mappers

import com.zbapp.core.crypto.CryptoCore
import com.zbapp.core.crypto.user.CryptoUserKeyRepository
import com.zbapp.core.models.raw.UserMeDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.core.models.user.SymmetricCryptoKeyData
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.models.user.UserMeData
import com.zbapp.core.utils.BaseRemoteModule
import kotlinx.serialization.encodeToString

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
            imageBase64 = model.imageBase64?.let { cryptoCore.decryptDataWithKey(it, userKey) },
            userName = model.userName?.let { cryptoCore.decryptDataWithKey(it, userKey) } ?: ""
        )
    }

    suspend fun core2Remote(
        model: UserMeData,
        privateKey: SymmetricCryptoKeyData,
        keyPairs: List<AsymmetricCryptoKeyData>
    ): UserMeDataRaw {
        val userKey = userKeyRepo.personalKey.content
        return UserMeDataRaw(
            imageBase64 = model.imageBase64?.let { cryptoCore.encryptDataWithKey(it, userKey) },
            userName = model.userName.let { cryptoCore.encryptDataWithKey(it, userKey) },
            privateKeyBackup = BaseRemoteModule.formatter.encodeToString(privateKey),
            asymmetricKeys = keyPairs.map { cryptoCore.encryptModel(it, userKey) }
        )
    }

}