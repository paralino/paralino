package app.paralino.core.db.main

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object MainDatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE groupEntity ADD COLUMN planExpiresAt TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE groupEntity ADD COLUMN provider TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("""
            CREATE TABLE IF NOT EXISTS userConfigEntity (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                ownerUserId TEXT NOT NULL,
                data TEXT
            )
        """.trimIndent())
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE groupEntity ADD COLUMN fallbackPlanType TEXT DEFAULT NULL")
        }
    }



    val migrations: List<Migration> = listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}