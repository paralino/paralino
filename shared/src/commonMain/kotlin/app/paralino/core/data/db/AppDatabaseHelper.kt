package app.paralino.core.data.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.paralino.core.provider.ICoroutineScopeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppDatabaseHelper(
    private val driverFactory: SqlDriverFactory,
    val scopeProvider: ICoroutineScopeProvider
) {
    private val defaultContext get() = scopeProvider.ioDispatcher
    private val database = AppDatabase(driverFactory.createDriver())

    val userQueries = database.userMeSQLQueries
    val subscriptionPlanQueries = database.plansSQLQueries
    val sharingContractQueries = database.sharingContractSQLQueries
    val personalCodeQueries = database.personalCodeSQLQueries
    val groupQueries = database.groupsSQLQueries
    val groupKeysQueries = database.groupKeysSQLQueries
    val memberKeysQueries = database.groupMemberKeysSQLQueries
    val placesQueries = database.placesSQLQueries
    val placeSettingsQueries = database.placeSettingsSQLQueries
    val memberQueries = database.membersSQLQueries
    val locationQueries = database.membersLocationsSQLQueries
    val configQueries = database.configDataSQLQueries
    val blocklistQueries = database.blocklistSQLQueries
    val userKeysQueries = database.userKeysSQLQueries
    val preKeysQueries = database.personalPreKeySQLQueries


    internal fun clearDatabase() {
        database.transaction {
            userQueries.clear()
        }
    }
    suspend fun transaction(query: AppDatabaseHelper.() -> Unit) =
        withContext(defaultContext) { database.transaction { query() } }
    suspend fun <T: Any> transactionWithResult(query: AppDatabaseHelper.() -> T?): T? =
        withContext(defaultContext) { database.transactionWithResult { query() } }

    suspend fun <T : Any> execute(query: AppDatabaseHelper.() -> Query<T>): T? =
        withContext(defaultContext) {
            query().executeAsOneOrNull()
        }

    fun <T : Any> mapToOneOrNull(query: AppDatabaseHelper.() -> Query<T>): Flow<T?> = query().asFlow().mapToOneOrNull(defaultContext)
    fun <T : Any> mapToList(query: AppDatabaseHelper.() -> Query<T>): Flow<List<T>> = query().asFlow().mapToList(defaultContext)
}