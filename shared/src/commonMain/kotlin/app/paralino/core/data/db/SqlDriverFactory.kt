package app.paralino.core.data.db

import app.cash.sqldelight.db.SqlDriver

expect class SqlDriverFactory {
    fun createMainDatabaseDriver(): SqlDriver
    fun createLogsDatabaseDriver(): SqlDriver
}