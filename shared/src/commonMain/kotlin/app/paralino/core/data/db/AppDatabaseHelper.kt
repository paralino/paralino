package app.paralino.core.data.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.paralino.core.provider.ICoroutineScopeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class ParalinoDatabaseHelper<T : Transacter> {
    abstract val defaultContext: CoroutineContext
    protected abstract val database: T
}

class LogsDatabaseHelper(
    private val driverFactory: SqlDriverFactory,
    val scopeProvider: ICoroutineScopeProvider
) : ParalinoDatabaseHelper<LogsDatabase>() {
    override val defaultContext: CoroutineContext get() = scopeProvider.ioDispatcher
    override val database = LogsDatabase(driverFactory.createLogsDatabaseDriver())

    val debugLogsQueries = database.debugLogsSQLQueries

    suspend fun transaction(query: LogsDatabaseHelper.() -> Unit) =
        withContext(defaultContext) { database.transaction { query() } }

    suspend fun <T : Any> transactionWithResult(query: LogsDatabaseHelper.() -> T?): T? =
        withContext(defaultContext) { database.transactionWithResult { query() } }
}

class AppDatabaseHelper(
    private val driverFactory: SqlDriverFactory,
    val scopeProvider: ICoroutineScopeProvider
) : ParalinoDatabaseHelper<MainAppDatabase>() {
    override val defaultContext: CoroutineContext get() = scopeProvider.ioDispatcher
    override val database = MainAppDatabase(driverFactory.createMainDatabaseDriver())

    val userQueries = database.userMeSQLQueries
    val userConfigQueries = database.userConfigSQLQueries
    val subscriptionPlanQueries = database.plansSQLQueries
    val sharingContractQueries = database.sharingContractSQLQueries
    val personalCodeQueries = database.personalCodeSQLQueries
    val groupQueries = database.groupsSQLQueries
    val groupKeysQueries = database.groupKeysSQLQueries
    val memberKeysQueries = database.groupMemberKeysSQLQueries
    val groupDeviceKeysQueries = database.groupDeviceKeysSQLQueries
    val groupDeviceIntegrityQueries = database.groupDeviceIntegritySQLQueries
    val placesQueries = database.placesSQLQueries
    val placeSettingsQueries = database.placeSettingsSQLQueries
    val placeActivityQueries = database.placeActivitySQLQueries
    val pendingPlaceEventsQueries = database.pendingPlaceEventsQueries
    val memberQueries = database.membersSQLQueries
    val locationQueries = database.membersLocationsSQLQueries
    val configQueries = database.configDataSQLQueries
    val blocklistQueries = database.blocklistSQLQueries
    val userKeysQueries = database.userKeysSQLQueries
    val personalGroupKeysQueries = database.personalGroupKeySQLQueries

    suspend fun transaction(query: AppDatabaseHelper.() -> Unit) =
        withContext(defaultContext) { database.transaction { query() } }

    suspend fun <T : Any> transactionWithResult(query: AppDatabaseHelper.() -> T?): T? =
        withContext(defaultContext) { database.transactionWithResult { query() } }
}

fun <T : Any, D: Transacter, R: ParalinoDatabaseHelper<D>> R.mapToList(query: R.() -> Query<T>): Flow<List<T>> =
    query().asFlow().mapToList(defaultContext)

suspend fun <D: Transacter, R: ParalinoDatabaseHelper<D>> R.execute(query: R.() -> Unit) =
    withContext(defaultContext) { query() }

suspend fun <T: Any, D: Transacter, R: ParalinoDatabaseHelper<D>> R.executeWithResult(query: R.() -> T?): T? =
    withContext(defaultContext) { query() }

fun <T : Any, D: Transacter, R: ParalinoDatabaseHelper<D>> R.mapToOneOrNull(query: R.() -> Query<T>): Flow<T?> =
    query().asFlow().mapToOneOrNull(defaultContext)