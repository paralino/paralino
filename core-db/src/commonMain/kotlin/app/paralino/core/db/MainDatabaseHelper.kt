package app.paralino.core.db

import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LogsDatabaseHelper(
    private val database: AppLogsDatabase
) {
    val debugLogsQueries = database.debugLogsDao()
    val accessLogQueries = database.accessLogDao()
}

class MainDatabaseHelper(
    private val onError: (message: String, error: Throwable) -> Unit = { _, _ -> },
    private val databaseFactory: () -> AppMainDatabase
) {
    private val currentDatabase = MutableStateFlow(databaseFactory())
    private val roomDatabase: AppMainDatabase get() = currentDatabase.value

    private val _generation = MutableStateFlow(0)

    val userDao get() = roomDatabase.userDao()
    val blocklistDao get() = roomDatabase.blocklistDao()
    val sharingContractsDao get() = roomDatabase.sharingContractsDao()
    val userConfigDao get() = roomDatabase.userConfigDao()
    val remoteConfigDao get() = roomDatabase.remoteConfigDao()
    val personalCodeDao get() = roomDatabase.personalCodeDao()
    val userKeysDao get() = roomDatabase.userKeysDao()
    val groupRelatedKeysDao get() = roomDatabase.groupRelatedKeysDao()
    val placeActivityDao get() = roomDatabase.placeActivityDao()
    val groupRelatedDao get() = roomDatabase.groupRelatedDao()
    val pendingLocationHistoryDao get() = roomDatabase.pendingLocationHistoryDao()
    val unconfirmedLocationHistoryDao get() = roomDatabase.unconfirmedLocationHistoryDao()
    val personalLocalHistoryKeyDao get() = roomDatabase.personalLocalHistoryKeyDao()
    val groupLocalHistoryKeyDao get() = roomDatabase.groupLocalHistoryKeyDao()
    val unsyncedGroupHistoryKeysDao get() = roomDatabase.unsyncedGroupHistoryKeysDao()
    val locationHistoryDao get() = roomDatabase.locationHistoryDao()
    val logoutDao get() = roomDatabase.logoutDao()
    val userDeviceDao get() = roomDatabase.userDeviceDao()
    val approximateSharedLocationDao get() = roomDatabase.approximateSharedLocationDao()
    val privacyZoneSharedLocationDao get() = roomDatabase.privacyZoneSharedLocationDao()
    val pendingGroupJoinWelcomeDao get() = roomDatabase.pendingGroupJoinWelcomeDao()
    val falsePlaceAlertLocationDao get() = roomDatabase.falsePlaceAlertLocationDao()
    val wifiAnchorDao get() = roomDatabase.wifiAnchorDao()

    /**
     * Needed for flows that are collected for a long time (eager singleton state flows - stateIn ...)
     * otherwise they stay bound to the old/closed instance and stop emitting.
     *
     * Errors after a reopen are just logged, so the flow does not break
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> observe(source: MainDatabaseHelper.() -> Flow<T>): Flow<T> {
        return _generation.flatMapLatest { generation ->
            if (generation == 0) {
                source()
            } else {
                source().catch { error ->
                    if (error is CancellationException) throw error
                    onError("Database flow failed after reopen ($generation)", error)
                }
            }
        }
    }

    /**
     * Used by the iOS location extension - LSE
     * LSE can be kept alive by the system for a long time since there is no multiprocess intercommuncation with Room,
     * database does not invalidate its internal cache when something changes in the main app and returns stale data.
     * This can cause location update to fail since encryption keys rotated in the main app are not reflected in LSE
     *
     * This will allow replacing current database instance with a new one (new SQLite connections) and closes current one, allowing queries already running on it to finish.
     *
     * Basically, iOS LSE gets new database instance on every push
     */
    fun reopen(
        scope: CoroutineScope,
        closePreviousAfter: Duration = 20.seconds
    ) {
        val nextDatabase = databaseFactory()
        val previousDatabase = currentDatabase.getAndUpdate { nextDatabase }
        _generation.update { it + 1 }

        scope.launch {
            delay(closePreviousAfter)
            try {
                previousDatabase.close()
            } catch (e: Exception) {
                onError("Unable to close previous database instance", e)
            }
        }
    }
}
