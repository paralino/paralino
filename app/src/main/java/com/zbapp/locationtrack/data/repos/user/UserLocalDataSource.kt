package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.crypto.user.ICryptoUserKeyLocalDataSource
import com.zbapp.core.models.raw.AsymmetricKeyDataRaw
import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.locationtrack.data.mappers.UserMapperLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val dao: UserDao,
    private val mapper: UserMapperLocal,
    private val keysLocal: ICryptoUserKeyLocalDataSource
): IUserLocalDataSource {

    override fun getMe(): Flow<UserMe?> {
        return dao.get().map { it?.let { mapper.local2Core(it) } }
    }

    override suspend fun update(data: UserMeRaw): UserMe {
        saveUserKeyPairs(data.data?.asymmetricKeys ?: emptyList())
        dao.save(mapper.remote2Local(data))
        return mapper.coreMapper.remote2Core(data)
    }

    suspend fun saveUserKeyPairs(keys: List<AsymmetricKeyDataRaw>) {
        keysLocal.recreateAllKeyPairs(keys)
    }

    override suspend fun clear() {
        dao.clear()
    }

}