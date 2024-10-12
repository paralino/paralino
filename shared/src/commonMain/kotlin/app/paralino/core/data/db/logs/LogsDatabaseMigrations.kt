package app.paralino.core.data.db.logs

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object LogsDatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE debugLogsEntity ADD COLUMN host TEXT DEFAULT NULL")
        }
    }

    val migrations: List<Migration> = listOf(MIGRATION_1_2)
}