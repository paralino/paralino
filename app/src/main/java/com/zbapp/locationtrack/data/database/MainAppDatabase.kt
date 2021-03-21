package com.zbapp.locationtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zbapp.locationtrack.data.models.location.LocationEntity
import com.zbapp.locationtrack.data.repos.location.LocationDao


@Database(
    entities = [
    LocationEntity::class
    ],
    version = MainAppDatabase.VERSION
)

@TypeConverters(
    LocalDateTimeConverter::class,
    LocalTimeConverter::class,
    LocalDateConverter::class,
)
abstract class MainAppDatabase: RoomDatabase() {

    abstract fun locationDao(): LocationDao

    companion object {
        const val VERSION = 2
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