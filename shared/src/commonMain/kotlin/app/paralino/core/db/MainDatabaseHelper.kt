package app.paralino.core.db

import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase

class LogsDatabaseHelper(
    private val database: AppLogsDatabase
) {
    val debugLogsQueries = database.debugLogsDao()
}

class MainDatabaseHelper(
    private val roomDatabase: AppMainDatabase
) {
    val userDao = roomDatabase.userDao()
    val blocklistDao = roomDatabase.blocklistDao()
    val sharingContractsDao = roomDatabase.sharingContractsDao()
    val userConfigDao = roomDatabase.userConfigDao()
    val remoteConfigDao = roomDatabase.remoteConfigDao()
    val personalCodeDao = roomDatabase.personalCodeDao()
    val userKeysDao = roomDatabase.userKeysDao()
    val groupRelatedKeysDao = roomDatabase.groupRelatedKeysDao()
    val placeActivityDao = roomDatabase.placeActivityDao()
    val groupRelatedDao = roomDatabase.groupRelatedDao()
}