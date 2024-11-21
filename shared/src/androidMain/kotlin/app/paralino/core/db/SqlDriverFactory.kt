package app.paralino.core.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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
    }

    actual fun createLogsDatabaseBuilder(): RoomDatabase.Builder<AppLogsDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("paralino_logs.db")
        return Room.databaseBuilder<AppLogsDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}