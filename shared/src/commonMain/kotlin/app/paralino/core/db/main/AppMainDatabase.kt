package app.paralino.core.db.main

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.paralino.core.db.SqlDriverFactory
import app.paralino.core.db.main.location.SharingContractDao
import app.paralino.core.db.main.location.SharingContractEntity
import app.paralino.core.db.main.members.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.members.GroupDeviceKeysEntity
import app.paralino.core.db.main.members.GroupKeysEntity
import app.paralino.core.db.main.members.GroupMembersKeysEntity
import app.paralino.core.db.main.members.GroupRelatedKeysDao
import app.paralino.core.db.main.members.PersonalGroupKeyEntity
import app.paralino.core.db.main.places.PlaceActivityDao
import app.paralino.core.db.main.places.PlaceActivityEntity
import app.paralino.core.db.main.user.BlocklistDao
import app.paralino.core.db.main.user.BlocklistEntity
import app.paralino.core.db.main.user.PersonalCodeDao
import app.paralino.core.db.main.user.PersonalCodeEntity
import app.paralino.core.db.main.user.RemoteConfigDao
import app.paralino.core.db.main.user.RemoteConfigEntity
import app.paralino.core.db.main.user.SubscriptionPlanDao
import app.paralino.core.db.main.user.SubscriptionPlanEntity
import app.paralino.core.db.main.user.UserConfigDao
import app.paralino.core.db.main.user.UserConfigEntity
import app.paralino.core.db.main.user.UserDao
import app.paralino.core.db.main.user.UserEntity
import app.paralino.core.db.main.user.UserKeysDao
import app.paralino.core.db.main.user.UserKeysEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        SubscriptionPlanEntity::class,
        UserEntity::class,
        BlocklistEntity::class,
        SharingContractEntity::class,
        UserConfigEntity::class,
        RemoteConfigEntity::class,
    PersonalCodeEntity::class,
    UserKeysEntity::class,
    PersonalGroupKeyEntity::class,
    GroupKeysEntity::class,
    GroupMembersKeysEntity::class,
    GroupDeviceKeysEntity::class,
    GroupDeviceIntegrityEntity::class,
    PlaceActivityEntity::class
    ],
    version = 4
)
@ConstructedBy(AppMainDatabaseConstructor::class)
abstract class AppMainDatabase : RoomDatabase() {
    abstract fun subscriptionPlanDao(): SubscriptionPlanDao
    abstract fun userDao(): UserDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun sharingContractsDao(): SharingContractDao
    abstract fun userConfigDao(): UserConfigDao
    abstract fun remoteConfigDao(): RemoteConfigDao
    abstract fun personalCodeDao(): PersonalCodeDao
    abstract fun userKeysDao(): UserKeysDao
    abstract fun groupRelatedKeysDao(): GroupRelatedKeysDao
    abstract fun placeActivityDao(): PlaceActivityDao

    companion object {
        fun create(builder: SqlDriverFactory): AppMainDatabase {
            return builder.createMainDatabaseBuilder()
                .addMigrations(*MainDatabaseMigrations.migrations.toTypedArray())
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppMainDatabaseConstructor : RoomDatabaseConstructor<AppMainDatabase> {
    override fun initialize(): AppMainDatabase
}