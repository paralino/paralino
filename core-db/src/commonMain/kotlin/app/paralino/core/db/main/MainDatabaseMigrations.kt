package app.paralino.core.db.main

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object MainDatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE groupEntity ADD COLUMN planExpiresAt TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE groupEntity ADD COLUMN provider TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("""
            CREATE TABLE IF NOT EXISTS userConfigEntity (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                ownerUserId TEXT NOT NULL,
                data TEXT
            )
        """.trimIndent())
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE groupEntity ADD COLUMN fallbackPlanType TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE placeEntity ADD COLUMN eventKeys TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE memberEntity ADD COLUMN settings_batteryAlertSettings TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS batteryAlertEvent (groupId TEXT NOT NULL, notifyTimeMillis INTEGER NOT NULL, PRIMARY KEY(groupId))")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userKeysEntity ADD COLUMN dataSalt TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userKeysEntity ADD COLUMN schemeVersion TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userEntity ADD COLUMN passwordUpdate TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS `localDeviceKeysEntity` (`id` TEXT NOT NULL, `locationId` TEXT NOT NULL, `memberKeyVersion` INTEGER NOT NULL, `deviceKeyVersion` INTEGER NOT NULL, `userKeyVersion` INTEGER NOT NULL, `encryptedSenderKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `shareDeviceStatus` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `saveToHistory` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `endTimeUTCOverride` TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `isEnabled` INTEGER NOT NULL DEFAULT 1")
            connection.execSQL("ALTER TABLE groupDeviceIntegrityEntity ADD COLUMN safetySignature TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE memberLocationEntity ADD COLUMN locationHistory TEXT DEFAULT NULL")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `pendingLocationHistoryEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` TEXT NOT NULL, `locationId` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `locationHistoryData` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `groupHistoryKeyId` INTEGER)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `unconfirmedLocationHistoryEntity` (`pendingUploadId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `locationHistoryData` TEXT NOT NULL, `historyKeyVersion` TEXT NOT NULL, PRIMARY KEY(`pendingUploadId`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `personalHistoryKeysEntity` (`id` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `groupHistoryKeysEntity` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `memberKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `personalHistoryEntity` (`id` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `locationId` TEXT, `historyData` TEXT NOT NULL, `historyKeyVersion` TEXT NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `groupHistoryEntity` (`id` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `groupId` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `locationId` TEXT, `historyKeyVersion` TEXT NOT NULL, `historyData` TEXT NOT NULL, PRIMARY KEY(`id`))")

            connection.execSQL("CREATE TABLE IF NOT EXISTS `personalLocalHistoryKeyEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, `isSynced` INTEGER NOT NULL)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `groupLocalHistoryKeyEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, `isActive` INTEGER NOT NULL)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `unsyncedGroupHistoryKeyEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` TEXT NOT NULL, `memberUserId` TEXT NOT NULL, `memberId` TEXT NOT NULL, `memberVerification` TEXT NOT NULL, `memberKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, `isSyncedOnce` INTEGER NOT NULL)")
            connection.execSQL("ALTER TABLE personalGroupKeyEntity ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")

            connection.execSQL("""
                CREATE TABLE IF NOT EXISTS `userDeviceEntity` (
                    `id` TEXT NOT NULL,
                    `pushTargetId` TEXT,
                    `sessionId` TEXT,
                    `deviceType` TEXT NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `deviceId` TEXT NOT NULL,
                    `locationPushToken` TEXT,
                    `batteryAlertSettings` TEXT DEFAULT NULL,
                    `encryptedSenderKey` TEXT,
                    `userKeyVersion` INTEGER,
                    `locationId` TEXT DEFAULT NULL,
                    `deviceData` TEXT,
                    `locationData` TEXT,
                    `status` TEXT,
                    `locationHistory` TEXT,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            connection.execSQL("ALTER TABLE placeEntity ADD COLUMN `isInPersonalSpace` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE placeEntity ADD COLUMN `userKeyVersion` INTEGER DEFAULT NULL")
            connection.execSQL("ALTER TABLE placeSettingsEntity ADD COLUMN `deviceId` TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE placeSettingsEntity ADD COLUMN `isInPersonalSpace` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE placeActivityEntity ADD COLUMN `isInPersonalSpace` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(connection: SQLiteConnection) {
            // 1. Create temporary table
            connection.execSQL("""
                CREATE TABLE IF NOT EXISTS `userConfigEntity_new` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `type` TEXT NOT NULL,
                    `data` TEXT
                )
            """.trimIndent())

            // 2. Copy data, setting id to the previous type
            connection.execSQL("""
                INSERT INTO `userConfigEntity_new` (id, type, data)
                SELECT id, type, data FROM `userConfigEntity`
            """.trimIndent())

            // 3. Drop old table
            connection.execSQL("DROP TABLE `userConfigEntity`")

            // 4. Rename new table
            connection.execSQL("ALTER TABLE `userConfigEntity_new` RENAME TO `userConfigEntity`")
        }
    }


    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userDeviceEntity ADD COLUMN isUsingNotificationSockets INTEGER DEFAULT NULL")
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE subscriptionPlanEntity ADD COLUMN updatedAt TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE subscriptionPlanEntity ADD COLUMN cancelledAt TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userDeviceEntity ADD COLUMN unifiedPushConfig TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE userDeviceEntity ADD COLUMN pushProvider TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE userDeviceEntity DROP COLUMN isUsingNotificationSockets")
        }
    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userDeviceEntity DROP COLUMN unifiedPushConfig")
            connection.execSQL("ALTER TABLE userDeviceEntity ADD COLUMN pushProviderConfig TEXT DEFAULT NULL")
        }
    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `isNotifyOthersOn` INTEGER DEFAULT NULL")
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `sharingDurationPreset` INTEGER DEFAULT NULL")
        }
    }

    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userEntity DROP COLUMN email")
        }
    }

    val migrations: List<Migration> = listOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21
    )
}