package app.paralino.core.models.mappers

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.LocationHistoryKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import app.paralino.core.db.main.history.GroupHistoryEntity
import app.paralino.core.db.main.history.PersonalHistoryEntity
import app.paralino.core.logg
import app.paralino.core.models.CryptoResource
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.raw.LocationHistoryReadEntry
import app.paralino.core.models.raw.LocationHistoryReadKey
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
                it.toReadModel()
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
                it.toReadModel()
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
            remote2Core(
                groupId = groupId,
                memberUserId = memberUserId,
                historyKey = keys.firstOrNull {
                    it.historyKeyVersion == entry.keyVersion
                },
                entry = entry
            )
        }

        return LocationHistoryModelForUser(
            groupId = groupId,
            memberUserId = memberUserId,
            history = history
        )
    }

    suspend fun remote2Core(
        groupId: String,
        memberUserId: String,
        historyKey: LocationHistoryKey.Valid?,
        entry: LocationHistoryReadEntry
    ): LocationHistoryModelForDevice {
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

        return LocationHistoryModelForDevice(
            locationId = entry.locationId,
            entryId = entry.id,
            dateKey = entry.dateKey,
            historyKeyVersion = entry.keyVersion,
            isCachedLocation = false,
            history = history
        )
    }

    fun remote2LocalGroup(
        groupId: String,
        memberUserId: String,
        model: LocationHistoryReadEntry
    ): GroupHistoryEntity {
        return GroupHistoryEntity(
            id = model.id,
            dateIdentifier = model.dateKey,
            groupId = groupId,
            ownerUserId = memberUserId,
            locationId = model.locationId,
            deviceId = model.deviceId,
            historyKeyVersion = model.keyVersion,
            historyData = model.historyData
        )
    }

    fun remote2LocalGroup(
        groupId: String,
        memberUserId: String,
        model: LocationHistoryReadKey
    ): GroupHistoryKeyEntity {
        return GroupHistoryKeyEntity(
            id = groupId + memberUserId + model.keyVersion,
            groupId = groupId,
            ownerUserId = memberUserId,
            memberKeyVersion = model.memberKeyVersion,
            historyKeyVersion = model.keyVersion,
            encryptedHistoryKey = model.encryptedKey
        )
    }

    fun local2RemoteGroup(
        model: GroupHistoryEntity
    ): LocationHistoryReadEntry {
        return LocationHistoryReadEntry(
            id = model.id,
            dateKey = model.dateIdentifier,
            locationId = model.locationId,
            deviceId = model.deviceId,
            keyVersion = model.historyKeyVersion,
            historyData = model.historyData
        )
    }

    fun remote2LocalPersonal(
        model: LocationHistoryReadEntry
    ): PersonalHistoryEntity {
        return PersonalHistoryEntity(
            id = model.id,
            dateIdentifier = model.dateKey,
            locationId = model.locationId,
            deviceId = model.deviceId,
            historyKeyVersion = model.keyVersion,
            historyData = model.historyData
        )
    }

    fun remote2LocalPersonal(
        model: LocationHistoryReadKey
    ): PersonalHistoryKeyEntity {
        return PersonalHistoryKeyEntity(
            id = model.keyVersion,
            userKeyVersion = model.memberKeyVersion,
            historyKeyVersion = model.keyVersion,
            encryptedHistoryKey = model.encryptedKey
        )
    }

    fun local2RemotePersonal(
        model: PersonalHistoryEntity
    ): LocationHistoryReadEntry {
        return LocationHistoryReadEntry(
            id = model.id,
            dateKey = model.dateIdentifier,
            locationId = model.locationId,
            deviceId = model.deviceId,
            keyVersion = model.historyKeyVersion,
            historyData = model.historyData
        )
    }
}