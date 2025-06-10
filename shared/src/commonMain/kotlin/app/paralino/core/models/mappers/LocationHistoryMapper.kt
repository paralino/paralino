package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.LocationHistoryKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import app.paralino.core.db.main.location.GroupHistoryEntity
import app.paralino.core.db.main.location.PersonalHistoryEntity
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.raw.LocationHistoryReadEntry
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.DecryptionException

class LocationHistoryMapper(
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val appPrefs: AppPrefs
) {

    suspend fun local2CorePersonal(
        keys: List<PersonalHistoryKeyEntity>,
        entries: List<PersonalHistoryEntity>
    ): LocationHistoryModelForUser {
        val actualKeys = keys
            .filter { key -> entries.any { it.historyKeyVersion == key.historyKeyVersion } }
            .distinctBy { it.historyKeyVersion }
            .mapNotNull { cryptoKEKRepo.parseLocationHistoryKey(keyRaw = it) as? LocationHistoryKey.Valid }

        return remote2Core(
            groupId = UserGroup.groupPersonalDevicesId,
            memberUserId = appPrefs.userId,
            keys = actualKeys,
            entries = entries.map {
                LocationHistoryReadEntry(
                    id = it.id,
                    dateKey = it.dateIdentifier,
                    deviceId = it.deviceId,
                    locationId = it.locationId,
                    keyVersion = it.historyKeyVersion,
                    historyData = it.historyData,
                )
            }
        )
    }

    suspend fun local2CoreGroup(
        groupId: String,
        memberUserId: String,
        keys: List<GroupHistoryKeyEntity>,
        entries: List<GroupHistoryEntity>
    ): LocationHistoryModelForUser {

        val actualKeys = keys
            .filter { key -> entries.any { it.historyKeyVersion == key.historyKeyVersion } }
            .distinctBy { it.historyKeyVersion }
            .mapNotNull { cryptoKEKRepo.parseLocationHistoryKey(keyRaw = it) as? LocationHistoryKey.Valid }

        return remote2Core(
            groupId = groupId,
            memberUserId = memberUserId,
            keys = actualKeys,
            entries = entries.map {
                LocationHistoryReadEntry(
                    id = it.id,
                    dateKey = it.dateIdentifier,
                    deviceId = it.deviceId,
                    locationId = it.locationId,
                    keyVersion = it.historyKeyVersion,
                    historyData = it.historyData,
                )
            }
        )
    }

    suspend fun remote2Core(
        groupId: String,
        memberUserId: String,
        keys: List<LocationHistoryKey.Valid>,
        entries: List<LocationHistoryReadEntry>
    ): LocationHistoryModelForUser {
        val history = entries.map { entry ->

            val historyKey = keys.firstOrNull {
                it.historyKeyVersion == entry.keyVersion
            }

            val history = if (historyKey == null) {
                CryptoResource.MissingKey
            } else {
                if (entry.historyData.isBlank()) {
                    CryptoResource.NotInitialized
                } else {
                    try {
                        cryptoCore.decryptDataWithKey(
                            data = entry.historyData,
                            key = historyKey.key
                        ).let {
                            BaseRemoteModule.locationHistoryFormatter
                                .decodeFromString<LocationHistoryModel>(it)
                        }.let {
                            CryptoResource.Ready(it)
                        }
                    } catch (e: Exception) {
                        if (historyKey.historyKeyVersion != entry.keyVersion) {
                            logg.w(
                                tag = "tag_crypto_key_group_member",
                                message = "Missing history key (entry: ${entry.keyVersion} - key: ${historyKey.historyKeyVersion}) ${e.message}"
                            )
                            CryptoResource.MissingKey
                        } else {
                            logg.tag("tag_crypto_key_group_member")
                                .e(
                                    message = "Error decrypting location history data for user ${memberUserId} in group ${groupId}\n" +
                                            "History key version: ${historyKey.memberKeyVersion} - ${e::class}: ${e.message}",
                                    t = DecryptionException(
                                        type = "historyData",
                                        message = "Error decrypting location history data: ${e::class}: ${e.message}",
                                        cause = e
                                    )
                                )
                            CryptoResource.WrongKeyOrData
                        }
                    }
                }
            }

            LocationHistoryModelForDevice(
                locationId = entry.locationId,
                history = history
            )
        }

        return LocationHistoryModelForUser(
            groupId = groupId,
            memberUserId = memberUserId,
            history = history
        )
    }
}