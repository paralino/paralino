package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.crypto.user.ICryptoUserKeyLocalDataSource
import com.zbapp.core.models.raw.UserCryptoKeyRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.locationtrack.data.config.Config
import com.zbapp.locationtrack.data.mappers.UserKeyMapperLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

    override fun userKeyState(): Flow<Boolean> {
        return dao.listenToKey()
            .map {
                try {
                    userKeyMapper.mapKeyPair(it!!, getCachedUserKey())
                    true
                } catch (e: Exception) {
                    false
                }
            }
            .catch { emit(false) }
            .distinctUntilChanged()
    }

    override suspend fun clearAllKeys() {
        appConfig.userKey = ""
        dao.clear()
    }

    override suspend fun deleteKey(id: String) {
        dao.delete(id)
    }
}