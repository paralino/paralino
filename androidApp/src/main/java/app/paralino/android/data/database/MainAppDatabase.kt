package app.paralino.android.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.paralino.android.data.models.group.GroupKeyEntity
import app.paralino.android.data.models.group.GroupMemberKeyEntity
import app.paralino.android.data.models.group.UserGroupEntity
import app.paralino.android.data.models.location.MemberEntity
import app.paralino.android.data.models.location.MemberLocationEntity
import app.paralino.android.data.models.location.SharingContractEntity
import app.paralino.android.data.models.places.GroupPlaceEntity
import app.paralino.android.data.models.places.GroupPlaceSettingsEntity
import app.paralino.android.data.models.user.PersonalCodeEntity
import app.paralino.android.data.models.user.UserEntity
import app.paralino.android.data.repos.group.GroupKeysDao
import app.paralino.android.data.repos.group.UserGroupDao
import app.paralino.android.data.repos.location.MemberDao
import app.paralino.android.data.repos.location.SharingContractsDao
import app.paralino.android.data.repos.personal_code.PersonalCodeDao
import app.paralino.android.data.repos.places.GroupPlacesDao
import app.paralino.android.data.repos.user.UserDao


@Database(
    entities = [
        MemberEntity::class,
        MemberLocationEntity::class,
        SharingContractEntity::class,
        UserEntity::class,
        UserGroupEntity::class,
        PersonalCodeEntity::class,
        GroupKeyEntity::class,
        GroupMemberKeyEntity::class,
        GroupPlaceEntity::class,
        GroupPlaceSettingsEntity::class
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
    abstract fun placesDao(): GroupPlacesDao

    companion object {
        const val VERSION = 47
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