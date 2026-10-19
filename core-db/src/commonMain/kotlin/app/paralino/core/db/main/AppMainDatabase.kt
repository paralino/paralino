package app.paralino.core.db.main

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.paralino.core.db.SqlDriverFactory
import app.paralino.core.db.main.blocklist.BlocklistDao
import app.paralino.core.db.main.blocklist.BlocklistEntity
import app.paralino.core.db.main.common.LogoutDao
import app.paralino.core.db.main.common.RemoteConfigDao
import app.paralino.core.db.main.common.RemoteConfigEntity
import app.paralino.core.db.main.common.SharingContractDao
import app.paralino.core.db.main.common.SharingContractEntity
import app.paralino.core.db.main.groups.BatteryAlertEventEntity
import app.paralino.core.db.main.groups.GroupEntity
import app.paralino.core.db.main.groups.GroupRelatedEntitiesDao
import app.paralino.core.db.main.groups.MemberEntity
import app.paralino.core.db.main.groups.MemberLocationEntity
import app.paralino.core.db.main.groups.PendingGroupJoinWelcomeDao
import app.paralino.core.db.main.groups.PendingGroupJoinWelcomeEntity
import app.paralino.core.db.main.keys.GroupDeviceIntegrityEntity
import app.paralino.core.db.main.keys.GroupDeviceKeysEntity
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.GroupKeysEntity
import app.paralino.core.db.main.keys.GroupMembersKeysEntity
import app.paralino.core.db.main.keys.GroupRelatedKeysDao
import app.paralino.core.db.main.keys.LocalGroupDeviceKeysEntity
import app.paralino.core.db.main.keys.GroupLocalHistoryKeyEntity
import app.paralino.core.db.main.keys.PendingKeyRefreshEntity
import app.paralino.core.db.main.keys.ArchivedGroupDeviceKeyEntity
import app.paralino.core.db.main.keys.PersonalGroupKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import app.paralino.core.db.main.keys.UserKeysDao
import app.paralino.core.db.main.keys.UserKeysEntity
import app.paralino.core.db.main.history.GroupHistoryEntity
import app.paralino.core.db.main.history.GroupLocalHistoryKeyDao
import app.paralino.core.db.main.history.LocationHistoryDao
import app.paralino.core.db.main.history.PendingLocationHistoryDao
import app.paralino.core.db.main.history.PendingLocationHistoryEntity
import app.paralino.core.db.main.history.PersonalHistoryEntity
import app.paralino.core.db.main.history.PersonalLocalHistoryKeyDao
import app.paralino.core.db.main.history.UnconfirmedLocationHistoryEntity
import app.paralino.core.db.main.history.UnconfirmedUploadHistoryDao
import app.paralino.core.db.main.history.UnsyncedGroupHistoryKeyDao
import app.paralino.core.db.main.history.UnsyncedGroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalLocalHistoryKeyEntity
import app.paralino.core.db.main.location.ApproximateSharedLocationDao
import app.paralino.core.db.main.location.ApproximateSharedLocationEntity
import app.paralino.core.db.main.places.PendingPlaceEventsEntity
import app.paralino.core.db.main.places.PlaceActivityDao
import app.paralino.core.db.main.places.PlaceActivityEntity
import app.paralino.core.db.main.places.PlaceEntity
import app.paralino.core.db.main.places.PlaceSettingsEntity
import app.paralino.core.db.main.privacyzones.PrivacyZoneEntity
import app.paralino.core.db.main.location.PrivacyZoneSharedLocationEntity
import app.paralino.core.db.main.location.PrivacyZoneSharedLocationDao
import app.paralino.core.db.main.user.PersonalCodeDao
import app.paralino.core.db.main.user.PersonalCodeEntity
import app.paralino.core.db.main.user.SubscriptionPlanEntity
import app.paralino.core.db.main.user.UserConfigDao
import app.paralino.core.db.main.user.UserConfigEntity
import app.paralino.core.db.main.user.UserDao
import app.paralino.core.db.main.user.UserDeviceDao
import app.paralino.core.db.main.user.UserDeviceEntity
import app.paralino.core.db.main.user.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        SubscriptionPlanEntity::class,
        UserEntity::class,
        UserDeviceEntity::class,
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
        LocalGroupDeviceKeysEntity::class,
        PlaceActivityEntity::class,
        GroupEntity::class,
        MemberEntity::class,
        MemberLocationEntity::class,
        PlaceEntity::class,
        PlaceSettingsEntity::class,
        PendingPlaceEventsEntity::class,
        BatteryAlertEventEntity::class,
        PendingGroupJoinWelcomeEntity::class,
        PendingLocationHistoryEntity::class,
        PersonalHistoryKeyEntity::class,
        PersonalHistoryEntity::class,
        GroupHistoryKeyEntity::class,
        GroupHistoryEntity::class,
        PersonalLocalHistoryKeyEntity::class,
        GroupLocalHistoryKeyEntity::class,
        UnconfirmedLocationHistoryEntity::class,
        UnsyncedGroupHistoryKeyEntity::class,
        ApproximateSharedLocationEntity::class,
        PrivacyZoneEntity::class,
        PrivacyZoneSharedLocationEntity::class,
        PendingKeyRefreshEntity::class,
        ArchivedGroupDeviceKeyEntity::class,
    ],
    version = 28,
    exportSchema = true
)
@ConstructedBy(AppMainDatabaseConstructor::class)
abstract class AppMainDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userDeviceDao(): UserDeviceDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun sharingContractsDao(): SharingContractDao
    abstract fun userConfigDao(): UserConfigDao
    abstract fun remoteConfigDao(): RemoteConfigDao
    abstract fun personalCodeDao(): PersonalCodeDao
    abstract fun userKeysDao(): UserKeysDao
    abstract fun groupRelatedKeysDao(): GroupRelatedKeysDao
    abstract fun placeActivityDao(): PlaceActivityDao
    abstract fun groupRelatedDao(): GroupRelatedEntitiesDao
    abstract fun pendingLocationHistoryDao(): PendingLocationHistoryDao
    abstract fun unconfirmedLocationHistoryDao(): UnconfirmedUploadHistoryDao
    abstract fun personalLocalHistoryKeyDao(): PersonalLocalHistoryKeyDao
    abstract fun groupLocalHistoryKeyDao(): GroupLocalHistoryKeyDao
    abstract fun locationHistoryDao(): LocationHistoryDao
    abstract fun unsyncedGroupHistoryKeysDao(): UnsyncedGroupHistoryKeyDao
    abstract fun logoutDao(): LogoutDao
    abstract fun approximateSharedLocationDao(): ApproximateSharedLocationDao
    abstract fun privacyZoneSharedLocationDao(): PrivacyZoneSharedLocationDao
    abstract fun pendingGroupJoinWelcomeDao(): PendingGroupJoinWelcomeDao

    companion object {
        fun create(builder: SqlDriverFactory): AppMainDatabase {
            return builder.createMainDatabaseBuilder()
                .addMigrations(*MainDatabaseMigrations.migrations.toTypedArray())
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }
    }
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppMainDatabaseConstructor : RoomDatabaseConstructor<AppMainDatabase> {
    override fun initialize(): AppMainDatabase
}