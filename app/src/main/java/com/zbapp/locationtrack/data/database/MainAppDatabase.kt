package com.zbapp.locationtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(
    entities = [

    ],
    version = MainAppDatabase.VERSION
)

@TypeConverters(
)
abstract class MainAppDatabase: RoomDatabase() {

    companion object {
        const val VERSION = 24
        const val DB_NAME = "app_main_db"

        fun create(context: Context): MainAppDatabase {
            return Room.databaseBuilder(
                context, MainAppDatabase::class.java, DB_NAME
            )
//                .addMigrations(*migrations)
                .fallbackToDestructiveMigration().build()
        }

    }

}