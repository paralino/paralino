package app.paralino.core.db.main

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object MainDatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {

        }
    }

    val migrations: List<Migration> = listOf(MIGRATION_1_2)
}