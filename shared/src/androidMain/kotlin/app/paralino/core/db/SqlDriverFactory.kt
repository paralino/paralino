package app.paralino.core.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.paralino.core.data.db.MainAppDatabase
import app.paralino.core.db.logs.AppLogsDatabase

actual class SqlDriverFactory(private val context: Context) {
    actual fun createMainDatabaseDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = MainAppDatabase.Schema,
            context = context,
            name = "paralino_main.db",
            callback = object : AndroidSqliteDriver.Callback(MainAppDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    setPragma(db, "JOURNAL_MODE = WAL")
                    setPragma(db, "SYNCHRONOUS = 2")
                }

                private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
                    val cursor = db.query("PRAGMA $pragma")
                    cursor.moveToFirst()
                    cursor.close()
                }
            }
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