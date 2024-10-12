package app.paralino.core.db

import androidx.room.RoomDatabase
import app.cash.sqldelight.db.SqlDriver
import app.paralino.core.db.logs.AppLogsDatabase

expect class SqlDriverFactory {
    fun createMainDatabaseDriver(): SqlDriver
    fun createLogsDatabaseBuilder(): RoomDatabase.Builder<AppLogsDatabase>
}
