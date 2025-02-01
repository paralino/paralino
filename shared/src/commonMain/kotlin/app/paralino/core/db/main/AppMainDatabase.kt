package app.paralino.core.db.main

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.paralino.core.db.SqlDriverFactory
import app.paralino.core.db.main.groups.GroupEntity
import app.paralino.core.db.main.groups.GroupRelatedEntitiesDao
import app.paralino.core.db.main.groups.MemberEntity
import app.paralino.core.db.main.groups.MemberLocationEntity
import app.paralino.core.db.main.common.SharingContractDao
import app.paralino.core.db.main.common.SharingContractEntity
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.keys.GroupDeviceKeysEntity
import app.paralino.core.db.main.keys.GroupKeysEntity
import app.paralino.core.db.main.keys.GroupMembersKeysEntity
import app.paralino.core.db.main.keys.GroupRelatedKeysDao
import app.paralino.core.db.main.keys.PersonalGroupKeyEntity
import app.paralino.core.db.main.places.PendingPlaceEventsEntity
import app.paralino.core.db.main.places.PlaceActivityDao
import app.paralino.core.db.main.places.PlaceActivityEntity
import app.paralino.core.db.main.places.PlaceEntity
import app.paralino.core.db.main.places.PlaceSettingsEntity
import app.paralino.core.db.main.blocklist.BlocklistDao
import app.paralino.core.db.main.blocklist.BlocklistEntity
import app.paralino.core.db.main.user.PersonalCodeDao
import app.paralino.core.db.main.user.PersonalCodeEntity
import app.paralino.core.db.main.common.RemoteConfigDao
import app.paralino.core.db.main.common.RemoteConfigEntity
import app.paralino.core.db.main.groups.BatteryAlertEventEntity
import app.paralino.core.db.main.user.SubscriptionPlanEntity
import app.paralino.core.db.main.user.UserConfigDao
import app.paralino.core.db.main.user.UserConfigEntity
import app.paralino.core.db.main.user.UserDao
import app.paralino.core.db.main.user.UserEntity
import app.paralino.core.db.main.keys.UserKeysDao
import app.paralino.core.db.main.keys.UserKeysEntity
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
        PlaceActivityEntity::class,
        GroupEntity::class,
        MemberEntity::class,
        MemberLocationEntity::class,
        PlaceEntity::class,
        PlaceSettingsEntity::class,
        PendingPlaceEventsEntity::class,
        BatteryAlertEventEntity::class
    ],
    version = 10
)
@ConstructedBy(AppMainDatabaseConstructor::class)
abstract class AppMainDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun sharingContractsDao(): SharingContractDao
    abstract fun userConfigDao(): UserConfigDao
    abstract fun remoteConfigDao(): RemoteConfigDao
    abstract fun personalCodeDao(): PersonalCodeDao
    abstract fun userKeysDao(): UserKeysDao
    abstract fun groupRelatedKeysDao(): GroupRelatedKeysDao
    abstract fun placeActivityDao(): PlaceActivityDao
    abstract fun groupRelatedDao(): GroupRelatedEntitiesDao

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