package com.zbapp.locationtrack.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zbapp.locationtrack.data.models.group.GroupKeyEntity
import com.zbapp.locationtrack.data.models.group.GroupMemberKeyEntity
import com.zbapp.locationtrack.data.models.group.UserGroupEntity
import com.zbapp.locationtrack.data.models.user.PersonalCodeEntity
import com.zbapp.locationtrack.data.models.location.MemberEntity
import com.zbapp.locationtrack.data.models.location.SharingContractEntity
import com.zbapp.locationtrack.data.models.user.UserEntity
import com.zbapp.locationtrack.data.models.user.UserKeyEntity
import com.zbapp.locationtrack.data.repos.group.GroupKeysDao
import com.zbapp.locationtrack.data.repos.group.UserGroupDao
import com.zbapp.locationtrack.data.repos.personal_code.PersonalCodeDao
import com.zbapp.locationtrack.data.repos.location.MemberDao
import com.zbapp.locationtrack.data.repos.location.SharingContractsDao
import com.zbapp.locationtrack.data.repos.user.UserDao
import com.zbapp.locationtrack.data.repos.user.UserKeyDao


@Database(
    entities = [
        MemberEntity::class,
        SharingContractEntity::class,
        UserEntity::class,
        UserGroupEntity::class,
        PersonalCodeEntity::class,
        GroupKeyEntity::class,
        GroupMemberKeyEntity::class,
        UserKeyEntity::class,
    ],
    version = MainAppDatabase.VERSION
)

@TypeConverters(
    LocalDateTimeConverter::class,
    LocalTimeConverter::class,
    LocalDateConverter::class,
    ListOfStringsConverters::class
)
abstract class MainAppDatabase : RoomDatabase() {

    abstract fun memberDao(): MemberDao
    abstract fun sharingContractDao(): SharingContractsDao
    abstract fun userDao(): UserDao
    abstract fun groupsDao(): UserGroupDao
    abstract fun personalCodeDao(): PersonalCodeDao
    abstract fun groupKeysDao(): GroupKeysDao
    abstract fun userKeysDao(): UserKeyDao

    companion object {
        const val VERSION = 23
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