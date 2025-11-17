package app.paralino.core.db

import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase

class LogsDatabaseHelper(
    private val database: AppLogsDatabase
) {
    val debugLogsQueries = database.debugLogsDao()
    val accessLogQueries = database.accessLogDao()
}

class MainDatabaseHelper(
    private val roomDatabase: AppMainDatabase
) {
    val userDao by lazy { roomDatabase.userDao() }
    val blocklistDao by lazy { roomDatabase.blocklistDao() }
    val sharingContractsDao by lazy { roomDatabase.sharingContractsDao() }
    val userConfigDao by lazy { roomDatabase.userConfigDao() }
    val remoteConfigDao by lazy { roomDatabase.remoteConfigDao() }
    val personalCodeDao by lazy { roomDatabase.personalCodeDao() }
    val userKeysDao by lazy { roomDatabase.userKeysDao() }
    val groupRelatedKeysDao by lazy { roomDatabase.groupRelatedKeysDao() }
    val placeActivityDao by lazy { roomDatabase.placeActivityDao() }
    val groupRelatedDao by lazy { roomDatabase.groupRelatedDao() }
    val pendingLocationHistoryDao by lazy { roomDatabase.pendingLocationHistoryDao() }
    val unconfirmedLocationHistoryDao by lazy { roomDatabase.unconfirmedLocationHistoryDao() }
    val personalLocalHistoryKeyDao by lazy { roomDatabase.personalLocalHistoryKeyDao() }
    val groupLocalHistoryKeyDao by lazy { roomDatabase.groupLocalHistoryKeyDao() }
    val unsyncedGroupHistoryKeysDao by lazy { roomDatabase.unsyncedGroupHistoryKeysDao() }
    val locationHistoryDao by lazy { roomDatabase.locationHistoryDao() }
    val logoutDao by lazy { roomDatabase.logoutDao() }
    val userDeviceDao by lazy { roomDatabase.userDeviceDao() }
}