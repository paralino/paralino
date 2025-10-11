package app.paralino.core.domain.location.history

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.LocalLocationHistoryKeyEntity
import app.paralino.core.db.main.location.PendingLocationHistoryEntity
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.group.UserGroup
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
    private val appBuildConfig: AppBuildConfig,
    private val appPrefs: AppPrefs
) {
    @Throws(CryptoException::class, MissingCryptoKeyException::class, CancellationException::class)
    suspend fun saveNewLocations(data: List<MemberGroupId>) {
        val userKey = cryptoUserKeysRepo.activePersonalKeyWithVersion()

        val entries = data.mapNotNull { entry ->
            if (entry.locationHistory != null) {
                val historyKey = database.pendingLocationHistoryDao.getLatestHistoryKey(
                    groupOrSpaceId = entry.groupId.id
                )

                // we are allowing saving personal history without a key
                // since key is generated before upload
                if (historyKey != null || entry.groupId.isPersonalSpace) {
                    val updatedEntry = getPossibleUpdatedEntry(
                        entry = entry,
                        userKey = userKey,
                        historyKey = historyKey
                    )

                    if (updatedEntry != null) {
                        updatedEntry
                    } else {
                        PendingLocationHistoryEntity(
                            id = 0,
                            groupId = entry.groupId.id,
                            locationId = entry.locationId,
                            locationHistoryData = cryptoCore.encryptDataWithKey(
                                data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                                    entry.locationHistory
                                ),
                                key = userKey.key
                            ),
                            userKeyVersion = userKey.version,
                            localGroupHistoryKeyId = historyKey?.id
                        )
                    }
                } else {
                    // history key is missing so we are skipping location point
                    null
                }
            } else {
                null
            }
        }

        database.pendingLocationHistoryDao.createPendingEntries(
            entries = entries
        )
    }

    private suspend fun getPossibleUpdatedEntry(
        entry: MemberGroupId,
        userKey: SymmetricCryptoKeyData,
        historyKey: LocalLocationHistoryKeyEntity?
    ): PendingLocationHistoryEntity? {
        if (entry.locationHistory == null) {
            return null
        }

        val lastPendingEntry = database.pendingLocationHistoryDao.getLatestPendingEntryOnce(
            groupOrSpaceId = entry.groupId.id
        )
        val isHistoryKeyValid = if (entry.groupId.isPersonalSpace) {
            // For personal space, no need to track history key since there is only one member
            // and history key is generated before upload, not before saving entry
            // in groups we need key before saving location
            true
        } else {
            lastPendingEntry?.localGroupHistoryKeyId == historyKey?.id
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
            PendingLocationHistoryEntity(
                id = lastPendingEntry.id,
                groupId = entry.groupId.id,
                locationId = entry.locationId,
                locationHistoryData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                        updatedEntry
                    ),
                    key = userKey.key
                ),
                userKeyVersion = userKey.version,
                localGroupHistoryKeyId = historyKey?.id
            )
        } else {
            null
        }
    }
}