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

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE placeEntity ADD COLUMN eventKeys TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE memberEntity ADD COLUMN settings_batteryAlertSettings TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS batteryAlertEvent (groupId TEXT NOT NULL, notifyTimeMillis INTEGER NOT NULL, PRIMARY KEY(groupId))")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userKeysEntity ADD COLUMN dataSalt TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userKeysEntity ADD COLUMN schemeVersion TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userEntity ADD COLUMN passwordUpdate TEXT DEFAULT NULL")
        }
    }


    val migrations: List<Migration> = listOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
    )
}