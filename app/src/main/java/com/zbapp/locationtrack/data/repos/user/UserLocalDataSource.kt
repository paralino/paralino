package com.zbapp.locationtrack.data.repos.user

import com.zbapp.core.models.user.UserMe
import com.zbapp.core.repo.user.IUserLocalDataSource
import com.zbapp.locationtrack.data.mappers.UserMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val dao: UserDao
): IUserLocalDataSource {

    override fun getMe(): Flow<UserMe> {
        return dao.get().map { UserMapper.local2Core(it) }
    }

    override suspend fun getMeOnce(): UserMe? {
        return dao.tryGetUser()?.let { UserMapper.local2Core(it) }
    }

    override suspend fun update(data: UserMe) {
        dao.save(UserMapper.core2Local(data))
    }

    override suspend fun clear() {
        dao.clear()
    }


}