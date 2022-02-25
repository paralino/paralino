package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.crypto.user.ICryptoUserKeyLocalDataSource
import com.zbapp.core.models.raw.AsymmetricKeyDataRaw
import com.zbapp.core.models.user.AsymmetricCryptoKeyData
import com.zbapp.locationtrack.appConfig
import com.zbapp.locationtrack.data.mappers.UserKeyMapper
import kotlinx.coroutines.flow.firstOrNull

class UserCryptoKeyLocalDataSource constructor(
    private val dao: UserKeyDao,
    private val userKeyMapper: UserKeyMapper
) : ICryptoUserKeyLocalDataSource {

    override fun getUserKey(): String {
        return appConfig.userKey
    }

    override fun saveUserKey(key: String) {
        appConfig.userKey = key
    }

    override fun clearUserKey() {
        appConfig.userKey = ""
    }

    override suspend fun recreateAllKeyPairs(keys: List<AsymmetricKeyDataRaw>) {
        dao.recreateAll(keys.map { userKeyMapper.mapKeyPair(it, getUserKey()) })
    }

    override suspend fun saveKeyPair(key: AsymmetricKeyDataRaw) {
        dao.createOrUpdate(userKeyMapper.mapKeyPair(key, getUserKey()))
    }

    override suspend fun getAllKeyPairs(): List<AsymmetricCryptoKeyData> {
        return dao.getAll().firstOrNull()?.map { userKeyMapper.mapKeyPair(it, getUserKey()) } ?: emptyList()
    }

    override suspend fun getLatestKeyPair(): AsymmetricCryptoKeyData? {
        val key = dao.getLatest() ?: return null
        return userKeyMapper.mapKeyPair(key, getUserKey())
    }

    override suspend fun clearAllKeys() {
        clearUserKey()
    }

}