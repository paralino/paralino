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
            connection.execSQL("ALTER TABLE groupDeviceIntegrityEntity ADD COLUMN safetySignature TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE memberLocationEntity ADD COLUMN locationHistory TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `saveToHistory` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `pendingLocationHistoryEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` TEXT NOT NULL, `locationId` TEXT NOT NULL, `locationHistoryData` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `historyKeyId` INTEGER NOT NULL)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `localLocationHistoryKeyEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `groupId` TEXT NOT NULL, `createdAt` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, `isSynced` INTEGER NOT NULL)")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `unconfirmedLocationHistoryEntity` (`pendingUploadId` TEXT NOT NULL, `groupId` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `locationHistoryData` TEXT NOT NULL, `historyKeyVersion` TEXT NOT NULL, PRIMARY KEY(`pendingUploadId`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `personalHistoryKeysEntity` (`id` TEXT NOT NULL, `userKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `groupHistoryKeysEntity` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `memberKeyVersion` INTEGER NOT NULL, `historyKeyVersion` TEXT NOT NULL, `encryptedHistoryKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `personalHistoryEntity` (`id` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `locationId` TEXT, `deviceId` TEXT, `historyData` TEXT NOT NULL, `historyKeyVersion` TEXT NOT NULL, PRIMARY KEY(`id`))")
            connection.execSQL("CREATE TABLE IF NOT EXISTS `groupHistoryEntity` (`id` TEXT NOT NULL, `dateIdentifier` TEXT NOT NULL, `groupId` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `locationId` TEXT, `deviceId` TEXT, `historyKeyVersion` TEXT NOT NULL, `historyData` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE sharingContractEntity ADD COLUMN `isEnabled` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("""
                CREATE TABLE IF NOT EXISTS `userDeviceEntity` (
                    `id` TEXT NOT NULL,
                    `pushTargetId` TEXT,
                    `sessionId` TEXT,
                    `deviceType` TEXT NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `deviceId` TEXT NOT NULL,
                    `locationPushToken` TEXT,
                    `encryptedSenderKey` TEXT,
                    `userKeyVersion` INTEGER,
                    `deviceData` TEXT,
                    `locationData` TEXT,
                    `status` TEXT,
                    `locationHistory` TEXT,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE placeEntity ADD COLUMN `isInPersonalSpace` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE placeEntity ADD COLUMN `userKeyVersion` INTEGER DEFAULT NULL")
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userDeviceEntity ADD COLUMN `locationId` TEXT DEFAULT NULL")
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
    )
}