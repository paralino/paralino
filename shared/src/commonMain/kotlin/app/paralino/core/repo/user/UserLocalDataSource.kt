package app.paralino.core.repo.user

import app.paralino.core.db.AppDatabaseHelper
import app.paralino.core.models.mappers.UserMapperLocal
import app.paralino.core.models.raw.BlocklistRaw
import app.paralino.core.models.raw.UserMeRaw
import app.paralino.core.models.raw.UserPlanRaw
import app.paralino.core.models.user.BlocklistModel
import app.paralino.core.models.user.UserMe
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserLocalDataSource(
    private val database: AppDatabaseHelper,
    private val mapper: UserMapperLocal,
) {

    fun getMeRaw(): Flow<UserMeRaw?> {
        return database.userDao.getUser()
            .map { it?.let { mapper.local2Remote(it) } }
            .distinctUntilChanged()
    }

    fun getMeRawPresentation(): Flow<Pair<UserMeRaw, UserMe>?> {
        return database.userDao.getUser()
            .map { it?.let { mapper.local2Remote(it) to mapper.local2Core(it) } }
            .distinctUntilChanged()
    }

    fun getBlocklist(): Flow<List<BlocklistModel>> {
        return database.blocklistDao.getBlocklist()
            .map { it.map { mapper.local2Core(it) } }
    }

    suspend fun recreateBlocklist(blocklist: List<BlocklistRaw>) {
        database.blocklistDao.recreateBlocklist(blocklist.map { mapper.remote2Local(it) })
    }

    suspend fun removeFromBlocklist(id: String) {
        database.blocklistDao.remove(id)
    }

    suspend fun update(data: UserMeRaw) {
        // always keep only one user
        database.userDao.recreateUser(mapper.remote2Local(data))
    }

    suspend fun savePlans(plans: List<UserPlanRaw>) {
        database.subscriptionPlanDao.recreatePlans(plans.map { mapper.remote2Local(it) })
    }

    fun getPlans(): Flow<List<UserPlanRaw>> {
        return database.subscriptionPlanDao.getAllPlans()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    suspend fun clear() {
        database.userDao.clearUserData(
            database.subscriptionPlanDao,
            database.blocklistDao
        )
    }
}