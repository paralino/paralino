package app.paralino.core.data.db.logs

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.paralino.core.data.db.SqlDriverFactory
import app.paralino.core.data.db.logs.debug.DebugLogsDao
import app.paralino.core.data.db.logs.debug.DebugLogsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [DebugLogsEntity::class],
    version = 2
)
@ConstructedBy(AppLogsDatabaseConstructor::class)
abstract class AppLogsDatabase : RoomDatabase() {
    abstract fun debugLogsDao(): DebugLogsDao

    companion object {
        fun create(builder: SqlDriverFactory): AppLogsDatabase {
            return builder.createLogsDatabaseBuilder()
                .addMigrations(*LogsDatabaseMigrations.migrations.toTypedArray())
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppLogsDatabaseConstructor : RoomDatabaseConstructor<AppLogsDatabase> {
    override fun initialize(): AppLogsDatabase
}