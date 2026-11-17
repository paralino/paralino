package app.paralino.core.db.logs

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.paralino.core.db.SqlDriverFactory
import app.paralino.core.db.logs.accesslog.AccessLogDao
import app.paralino.core.db.logs.accesslog.AccessLogEntity
import app.paralino.core.db.logs.debug.DebugLogsDao
import app.paralino.core.db.logs.debug.DebugLogsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        DebugLogsEntity::class,
        AccessLogEntity::class
    ],
    version = 3
)
@ConstructedBy(AppLogsDatabaseConstructor::class)
abstract class AppLogsDatabase : RoomDatabase() {
    abstract fun debugLogsDao(): DebugLogsDao
    abstract fun accessLogDao(): AccessLogDao

    companion object {
        fun create(builder: SqlDriverFactory): AppLogsDatabase {
            return builder.createLogsDatabaseBuilder()
                .addMigrations(*LogsDatabaseMigrations.migrations.toTypedArray())
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppLogsDatabaseConstructor : RoomDatabaseConstructor<AppLogsDatabase> {
    override fun initialize(): AppLogsDatabase
}