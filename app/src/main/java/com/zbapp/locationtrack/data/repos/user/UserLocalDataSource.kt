package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.locationtrack.data.mappers.UserMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val dao: UserDao,
    private val mapper: UserMapper
): IUserLocalDataSource {

    override fun getMe(): Flow<UserMe> {
        return dao.get().map { mapper.local2Remote(it) }
    }

    override suspend fun getMeOnce(): UserMe? {
        return dao.tryGetUser()?.let { mapper.local2Remote(it) }
    }

    override suspend fun update(data: UserMe) {
        dao.save(UserMapper.remote2Local(data))
    }

    override suspend fun clear() {
        dao.clear()
    }


}