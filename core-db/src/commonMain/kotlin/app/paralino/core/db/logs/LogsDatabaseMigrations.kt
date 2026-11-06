package app.paralino.core.db.logs

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object LogsDatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE debugLogsEntity ADD COLUMN host TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS `access_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `host` TEXT NOT NULL,
                        `access_id` TEXT NOT NULL,
                        `access_time_utc` TEXT NOT NULL
                    )
                """)
        }
    }

    val migrations: List<Migration> = listOf(MIGRATION_1_2, MIGRATION_2_3)
}