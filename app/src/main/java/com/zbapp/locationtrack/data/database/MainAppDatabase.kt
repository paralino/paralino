package com.zbapp.locationtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zbapp.locationtrack.data.models.location.LocationEntity
import com.zbapp.locationtrack.data.models.location.SharingContractEntity
import com.zbapp.locationtrack.data.models.user.UserEntity
import com.zbapp.locationtrack.data.repos.location.LocationDao
import com.zbapp.locationtrack.data.repos.location.SharingContractsDao
import com.zbapp.locationtrack.data.repos.user.UserDao


@Database(
    entities = [
    LocationEntity::class,
    SharingContractEntity::class,
    UserEntity::class
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
    abstract fun sharingContractDao(): SharingContractsDao
    abstract fun userDao(): UserDao

    companion object {
        const val VERSION = 7
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