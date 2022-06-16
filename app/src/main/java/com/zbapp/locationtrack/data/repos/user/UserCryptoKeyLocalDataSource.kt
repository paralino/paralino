package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.crypto.user.ICryptoUserKeyLocalDataSource
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.locationtrack.data.config.Config
import com.zbapp.locationtrack.data.mappers.UserKeyMapperLocal

class UserCryptoKeyLocalDataSource constructor(
    private val dao: UserKeyDao,
    private val userKeyMapper: UserKeyMapperLocal,
    private val appConfig: Config,
) : ICryptoUserKeyLocalDataSource {

    override fun getCachedUserKey(): String {
        return appConfig.userKey
    }

    override suspend fun saveKey(key: UserCryptoKeyRaw, decryptedMainKey: String) {
        appConfig.userKey = decryptedMainKey
        dao.recreateAll(userKeyMapper.remote2Local(key))
    }

    override suspend fun getKeyPair(): AsymmetricCryptoKeyData? {
        return dao.getKey()?.let { userKeyMapper.mapKeyPair(it, getCachedUserKey()) }
    }

    override suspend fun getMyKey(): UserCryptoKeyRaw? {
        return dao.getKey()?.let { userKeyMapper.local2Remote(it) }
    }

    override suspend fun clearAllKeys() {
        appConfig.userKey = ""
        dao.clear()
    }

    override suspend fun deleteKey(id: String) {
        dao.delete(id)
    }
}