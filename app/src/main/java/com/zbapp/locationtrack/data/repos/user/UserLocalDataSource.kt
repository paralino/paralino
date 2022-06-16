package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.models.raw.UserMeRaw
import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.locationtrack.data.mappers.UserMapperLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val dao: UserDao,
    private val mapper: UserMapperLocal,
): IUserLocalDataSource {

    override fun getMe(): Flow<UserMe?> {
        return dao.get().map { it?.let { mapper.local2Core(it) } }
    }

    override suspend fun update(data: UserMeRaw): UserMe {
        dao.save(mapper.remote2Local(data))
        return mapper.coreMapper.remote2Core(data)
    }

    override suspend fun clear() {
        dao.clear()
    }

}