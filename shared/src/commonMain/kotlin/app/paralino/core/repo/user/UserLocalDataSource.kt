package app.paralino.core.repo.user

import app.paralino.core.data.db.AppDatabaseHelper
import app.paralino.core.data.db.mapToList
import app.paralino.core.data.db.mapToOneOrNull
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

    fun getMe(): Flow<UserMe?> {
        return database.mapToOneOrNull { userQueries.getUser() }
            .map { it?.let { mapper.local2Core(it) } }
    }

    fun getMeRaw(): Flow<UserMeRaw?> {
        return database.mapToOneOrNull { userQueries.getUser() }
            .map { it?.let { mapper.local2Remote(it) } }
            .distinctUntilChanged()
    }

    fun getMeRawPresentation(): Flow<Pair<UserMeRaw, UserMe>?> {
        return database.mapToOneOrNull { userQueries.getUser() }
            .map { it?.let { mapper.local2Remote(it) to mapper.local2Core(it) } }
            .distinctUntilChanged()
    }

    fun getBlocklist(): Flow<List<BlocklistModel>> {
        return database.mapToList { blocklistQueries.getAll() }
            .map { it.map { mapper.local2Core(it) } }
    }

    suspend fun recreateBlocklist(blocklist: List<BlocklistRaw>) {
        database.blocklistQueries.transaction {
            database.blocklistQueries.clearAll()
            blocklist.forEach {
                database.blocklistQueries.createBlocklist(mapper.remote2Local(it))
            }
        }
    }

    suspend fun removeFromBlocklist(id: String) {
        database.blocklistQueries.remove(id)
    }

    suspend fun update(data: UserMeRaw) {
        // always keep only one user
        database.userQueries.transaction {
            database.userQueries.clear()
            database.userQueries.createUser(mapper.remote2Local(data))
        }
    }

    suspend fun savePlans(plans: List<UserPlanRaw>) {
        database.transaction {
            subscriptionPlanQueries.clearAll()
            plans.forEach {
                subscriptionPlanQueries.createPlan(mapper.remote2Local(it))
            }
        }
    }

    fun getPlans(): Flow<List<UserPlanRaw>> {
        return database.mapToList {
            subscriptionPlanQueries.getAllPlans()
        }
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    suspend fun clear() {
        database.transaction {
            userQueries.clear()
            blocklistQueries.clearAll()
            subscriptionPlanQueries.clearAll()
        }
    }
}