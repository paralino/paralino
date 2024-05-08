package app.paralino.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class SqlDriverFactory {
    actual fun createMainDatabaseDriver(): SqlDriver {
        return NativeSqliteDriver(MainAppDatabase.Schema, "paralino_main.db")
    }
    actual fun createLogsDatabaseDriver(): SqlDriver {
        return NativeSqliteDriver(LogsDatabase.Schema, "paralino_logs.db")
    }
}