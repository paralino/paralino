package com.zbapp.locationtrack.data.repos.user

import com.zbapp.location.models.user.UserMe
import com.zbapp.location.repo.user.IUserLocalDataSource
import com.zbapp.locationtrack.data.models.user.UserMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val dao: UserDao
): IUserLocalDataSource {

    override fun getMe(): Flow<UserMe> {
        return dao.get().map { UserMapper.map(it) }
    }

    override suspend fun getMeOnce(): UserMe? {
        return dao.tryGetUser()?.let { UserMapper.map(it) }
    }

    override suspend fun update(data: UserMe) {
        dao.save(UserMapper.mapReversed(data))
    }

    override suspend fun clear() {
        dao.clear()
    }


}