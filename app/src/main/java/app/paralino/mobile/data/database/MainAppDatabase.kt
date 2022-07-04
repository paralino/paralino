package app.paralino.mobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.paralino.mobile.data.models.group.GroupKeyEntity
import app.paralino.mobile.data.models.group.GroupMemberKeyEntity
import app.paralino.mobile.data.models.group.UserGroupEntity
import app.paralino.mobile.data.models.user.PersonalCodeEntity
import app.paralino.mobile.data.models.location.MemberEntity
import app.paralino.mobile.data.models.location.SharingContractEntity
import app.paralino.mobile.data.models.user.UserEntity
import app.paralino.mobile.data.models.user.UserKeyEntity
import app.paralino.mobile.data.repos.group.GroupKeysDao
import app.paralino.mobile.data.repos.group.UserGroupDao
import app.paralino.mobile.data.repos.personal_code.PersonalCodeDao
import app.paralino.mobile.data.repos.location.MemberDao
import app.paralino.mobile.data.repos.location.SharingContractsDao
import app.paralino.mobile.data.repos.user.UserDao
import app.paralino.mobile.data.repos.user.UserKeyDao


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
        const val VERSION = 26
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