package app.paralino.android.data.repos.user

import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import app.paralino.core.repo.user.IUserLocalDataSource
import app.paralino.android.data.mappers.UserMapperLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val dao: UserDao,
    private val mapper: UserMapperLocal,
) : IUserLocalDataSource {

    override fun getMe(): Flow<UserMe?> {
        return dao.get().map { it?.let { mapper.local2Core(it) } }
    }

    override fun getMeRaw(): Flow<UserMeRaw?> {
        return dao.get().map { it?.let { mapper.local2Remote(it) } }.distinctUntilChanged()
    }

    override fun getMeRawPresentation(): Flow<Pair<UserMeRaw, UserMe>?> {
        return dao.get().map { it?.let { mapper.local2Remote(it) to mapper.local2Core(it) } }.distinctUntilChanged()
    }

    override suspend fun update(data: UserMeRaw): UserMe {
        dao.save(mapper.remote2Local(data))
        return mapper.coreMapper.remote2Core(data)
    }

    override suspend fun clear() {
        dao.clear()
    }

}