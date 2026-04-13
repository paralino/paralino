package app.paralino.core.db

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver
import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase
import app.paralino.core.models.AppBuildConfig
import platform.Foundation.NSFileManager

actual class SqlDriverFactory(private val appBuildConfig: AppBuildConfig) {
    actual fun createMainDatabaseBuilder(): RoomDatabase.Builder<AppMainDatabase> {
        val documentDirectory = NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier(groupIdentifier = appBuildConfig.iOSAppGroup)
        val dbFile = requireNotNull(documentDirectory).path!!.let { "$it/paralino_main.db" }

        return Room.databaseBuilder<AppMainDatabase>(
            name = dbFile
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    }

    actual fun createLogsDatabaseBuilder(): RoomDatabase.Builder<AppLogsDatabase> {
        val documentDirectory = NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier(groupIdentifier = appBuildConfig.iOSAppGroup)
        val dbFile = requireNotNull(documentDirectory).path!!.let { "$it/paralino_logs.db" }

        return Room.databaseBuilder<AppLogsDatabase>(
            name = dbFile
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    }
}
