package app.paralino.core.repo.user

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.models.mappers.UserMapperLocal
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.user.UserMe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val database: AppDatabaseHelper,
    private val mapper: UserMapperLocal,
) : IUserLocalDataSource {

    override fun getMe(): Flow<UserMe?> {
        return database.mapToOneOrNull { userQueries.getUser() }
            .map { it?.let { mapper.local2Core(it) } }
    }

    override fun getMeRaw(): Flow<UserMeRaw?> {
        return database.mapToOneOrNull { userQueries.getUser() }
            .map { it?.let { mapper.local2Remote(it) } }
            .distinctUntilChanged()
    }

    override fun getMeRawPresentation(): Flow<Pair<UserMeRaw, UserMe>?> {
        return database.mapToOneOrNull { userQueries.getUser() }
            .map { it?.let { mapper.local2Remote(it) to mapper.local2Core(it) } }
            .distinctUntilChanged()
    }

    override suspend fun update(data: UserMeRaw) {
        database.userQueries.createUser(mapper.remote2Local(data))
    }

    override suspend fun clear() {
        database.userQueries.clear()
    }

}