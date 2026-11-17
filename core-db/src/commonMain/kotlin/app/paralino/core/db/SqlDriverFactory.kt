package app.paralino.core.db

import androidx.room3.RoomDatabase
import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase

expect class SqlDriverFactory {
    fun createMainDatabaseBuilder(): RoomDatabase.Builder<AppMainDatabase>
    fun createLogsDatabaseBuilder(): RoomDatabase.Builder<AppLogsDatabase>
}
