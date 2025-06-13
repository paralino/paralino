package app.paralino.core.domain.location

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.LocalLocationHistoryKeyEntity
import app.paralino.core.db.main.location.PendingLocationHistoryEntity
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.location.LocationHistoryPointModel
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.user.SymmetricCryptoKeyData
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
                    groupId = entry.groupId
                )

                if (historyKey != null) {
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
                            groupId = entry.groupId,
                            locationId = entry.locationId,
                            locationHistoryData = cryptoCore.encryptDataWithKey(
                                data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                                    entry.locationHistory
                                ),
                                key = userKey.key
                            ),
                            userKeyVersion = userKey.version,
                            localHistoryKeyId = historyKey.id
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
        historyKey: LocalLocationHistoryKeyEntity
    ): PendingLocationHistoryEntity? {
        if (entry.locationHistory == null)
            return null

        val lastPendingEntry = database.pendingLocationHistoryDao.getLatestPendingEntryOnce(
            groupId = entry.groupId
        )

        val lastEntryPoint =
            if (lastPendingEntry?.userKeyVersion == userKey.version && lastPendingEntry.localHistoryKeyId == historyKey.id) {
                try {
                    cryptoCore.decryptDataWithKey(
                        data = lastPendingEntry.locationHistoryData,
                        key = userKey.key
                    ).let {
                        BaseRemoteModule.locationHistoryFormatter.decodeFromString<LocationHistoryPointModel>(
                            it
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

        val updatedEntry =
            if (lastEntryPoint?.isStatusUnchanged(entry.locationHistory.status) == true) {
                if (entry.locationHistory.loc != null && lastEntryPoint.isLocationUnchanged(entry.locationHistory.loc) == true) {
                    lastEntryPoint.copy(
                        loc = lastEntryPoint.loc?.copy(
                            tB = entry.locationHistory.loc.tB
                        ),
                        status = lastEntryPoint.status.copy(
                            tB = entry.locationHistory.status.tB,
                        )
                    )
                } else {
                    null
                }
            } else {
                null
            }

        return if (updatedEntry != null && lastPendingEntry != null) {
            PendingLocationHistoryEntity(
                id = lastPendingEntry.id,
                groupId = entry.groupId,
                locationId = entry.locationId,
                locationHistoryData = cryptoCore.encryptDataWithKey(
                    data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                        updatedEntry
                    ),
                    key = userKey.key
                ),
                userKeyVersion = userKey.version,
                localHistoryKeyId = historyKey.id
            )
        } else {
            null
        }
    }
}