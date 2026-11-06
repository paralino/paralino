package app.paralino.core.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import app.paralino.core.db.logs.AppLogsDatabase
import app.paralino.core.db.main.AppMainDatabase

actual class SqlDriverFactory(private val context: Context) {
    actual fun createMainDatabaseBuilder(): RoomDatabase.Builder<AppMainDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("paralino_main.db")
        return Room.databaseBuilder<AppMainDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    }

    actual fun createLogsDatabaseBuilder(): RoomDatabase.Builder<AppLogsDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("paralino_logs.db")
        return Room.databaseBuilder<AppLogsDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    }
}