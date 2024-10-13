package app.paralino.core.db

import androidx.room.RoomDatabase
import app.cash.sqldelight.db.SqlDriver
import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase

expect class SqlDriverFactory {
    fun createMainDatabaseDriver(): SqlDriver
    fun createMainDatabaseBuilder(): RoomDatabase.Builder<AppMainDatabase>
    fun createLogsDatabaseBuilder(): RoomDatabase.Builder<AppLogsDatabase>
}
