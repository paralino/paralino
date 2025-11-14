package app.paralino.core.domain.location.history

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.history.PendingLocationHistoryEntity
import app.paralino.core.logg
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.user.SymmetricCryptoKeyData
import app.paralino.core.repo.location.LocationPerformanceSettingsType
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.CryptoException
import app.paralino.core.utils.exceptions.MissingCryptoKeyException
import kotlin.coroutines.cancellation.CancellationException

class SavePendingLocationUseCase(
    private val database: MainDatabaseHelper,
    private val cryptoCore: CryptoCore,
    private val cryptoUserKeysRepo: CryptoUserKeysLocalSource,
    private val appPrefs: AppPrefs
) {
    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun saveNewLocations(data: List<MemberGroupId>) {
        val userKey = cryptoUserKeysRepo.activePersonalKeyWithVersion()

        val entries = data.mapNotNull { entry ->
            getLocalEntry(
                userKey = userKey,
                entry = entry
            )
        }

        database.pendingLocationHistoryDao.createPendingEntries(
            entries = entries
        )
    }

    private suspend fun getLocalEntry(
        userKey: SymmetricCryptoKeyData,
        entry: MemberGroupId
    ): PendingLocationHistoryEntity? {
        if (entry.locationHistory == null) {
            return null
        }

        val groupHistoryKeyId = if (entry.groupId.isPersonalSpace) {
            null
        } else {
            // if history key is missing we are skipping location point
            database.groupLocalHistoryKeyDao.getActiveHistoryKey(
                groupId = entry.groupId.id
            )?.id ?: return null
        }

        val updatedEntry = getPossibleUpdatedEntry(
            entry = entry,
            userKey = userKey,
            groupHistoryKeyId = groupHistoryKeyId
        )

        return if (updatedEntry != null) {
            updatedEntry
        } else if (!entry.locationHistory.dayKey.isNullOrBlank()) {
            PendingLocationHistoryEntity(
                id = 0,
                groupId = entry.groupId.id,
                locationId = entry.locationId,
                dateIdentifier = entry.locationHistory.dayKey,
                locationHistoryData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                        entry.locationHistory
                    ),
                    key = userKey.key
                ),
                userKeyVersion = userKey.version,
                localGroupHistoryKeyId = groupHistoryKeyId
            )
        } else {
            logg.e("location_history", "Day key is null or blank: ${entry.locationHistory.status.startTime}")
            null
        }
    }

    private suspend fun getPossibleUpdatedEntry(
        entry: MemberGroupId,
        userKey: SymmetricCryptoKeyData,
        groupHistoryKeyId: Long?
    ): PendingLocationHistoryEntity? {
        if (entry.locationHistory == null) {
            return null
        }

        val lastPendingEntry = database.pendingLocationHistoryDao.getLatestPendingEntryOnce(
            groupOrSpaceId = entry.groupId.id
        )
        val isHistoryKeyValid = if (entry.groupId.isPersonalSpace) {
            // For personal space, no need to track history key since there is only one member
            // and history key is generated before upload, not before saving entry.
            // in groups we need key before saving location
            true
        } else {
            lastPendingEntry?.localGroupHistoryKeyId == groupHistoryKeyId
        }

        val lastEntryPoint =
            if (lastPendingEntry?.userKeyVersion == userKey.version && isHistoryKeyValid) {
                try {
                    cryptoCore.decryptDataWithKey(
                        data = lastPendingEntry.locationHistoryData,
                        key = userKey.key
                    ).let {
                        BaseRemoteModule.locationHistoryFormatter
                            .decodeFromString<LocationHistoryPointModel>(it)
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

        if (entry.locationHistory.dayKey != lastEntryPoint?.dayKey) {
            return null
        }

        val updatedEntry =
            if (lastEntryPoint?.isStatusUnchanged(entry.locationHistory.status) == true) {
                if (entry.locationHistory.loc != null) {
                    val mode = LocationPerformanceSettingsType.History.fromKey(key = appPrefs.locationSettingsHistoryPerformanceType)
                    if (lastEntryPoint.isLocationUnchanged(location = entry.locationHistory.loc, mode = mode) == true) {
                        lastEntryPoint.copy(
                            loc = lastEntryPoint.loc?.copy(
                                tB = entry.locationHistory.loc.tA
                            ),
                            status = lastEntryPoint.status.copy(
                                tB = entry.locationHistory.status.tA,
                            )
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }

        return if (updatedEntry != null && lastPendingEntry != null) {
            if (!updatedEntry.dayKey.isNullOrBlank()) {
                PendingLocationHistoryEntity(
                    id = lastPendingEntry.id,
                    groupId = entry.groupId.id,
                    locationId = entry.locationId,
                    dateIdentifier = updatedEntry.dayKey,
                    locationHistoryData = cryptoCore.encryptDataWithKey(
                        data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                            updatedEntry
                        ),
                        key = userKey.key
                    ),
                    userKeyVersion = userKey.version,
                    localGroupHistoryKeyId = groupHistoryKeyId
                )
            } else {
                logg.e("location_history", "Day key is null or blank: ${updatedEntry.status.startTime}")
                null
            }
        } else {
            null
        }
    }
}