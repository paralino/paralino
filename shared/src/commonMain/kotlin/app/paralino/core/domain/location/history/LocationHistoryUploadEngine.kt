package app.paralino.core.domain.location.history

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.LocalLocationHistoryKeyEntity
import app.paralino.core.db.main.location.UnconfirmedLocationHistoryEntity
import app.paralino.core.domain.LimitedExecutionRepository
import app.paralino.core.domain.crypto.GroupValidityVerification
import app.paralino.core.domain.crypto.IntegrityResult
import app.paralino.core.logg
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.location.LocationHistoryPointModelForMember
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.models.raw.HistoryUploadResponse
import app.paralino.core.models.raw.LocationHistoryGroupPayload
import app.paralino.core.models.raw.LocationHistoryPayloadEntry
import app.paralino.core.models.raw.LocationHistoryPersonalKeyPayload
import app.paralino.core.models.raw.LocationHistoryPersonalPayload
import app.paralino.core.models.raw.LocationHistoryUploadPayload
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.InvalidRequestException
import app.paralino.core.utils.extensions.isOlderThan
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

interface ILocationHistoryUploadEngine {
    abstract fun syncPendingEntriesInBackground(cacheTime: Duration)
    abstract suspend fun syncPendingEntriesNow(cacheTime: Duration): Result<Unit>
}

class IOSLocationHistoryUploadEngine : ILocationHistoryUploadEngine {
    override fun syncPendingEntriesInBackground(cacheTime: Duration) {}
    override suspend fun syncPendingEntriesNow(cacheTime: Duration): Result<Unit> =
        Result.failure(InvalidRequestException("Function not available from iOS extension"))
}

class LocationHistoryUploadEngine(
    private val cryptoUserKeysRepo: CryptoUserKeysLocalSource,
    private val validityVerification: GroupValidityVerification,
    private val historyMapper: PendingLocationHistoryMapper,
    private val groupLocalDataSource: UserGroupLocalDataSource,
    private val database: MainDatabaseHelper,
    private val scopeProvider: ICoroutineScopeProvider,
    private val cryptoCore: CryptoCore,
    private val functionsApi: IFunctionsApi,
    private val appPrefs: AppPrefs,
) : LimitedExecutionRepository(), ILocationHistoryUploadEngine {

    private val executionKey = "location_history_pending_sync"

    override fun isProcessing(key: String): Boolean {
        return super.isProcessing(key)
    }

    override fun syncPendingEntriesInBackground(cacheTime: Duration) {
        scopeProvider.userSessionIOScope.launch {
            syncPendingEntriesNow(cacheTime)
        }
    }

    override suspend fun syncPendingEntriesNow(cacheTime: Duration): Result<Unit> {

        try {
            startProcessing(executionKey)
        } catch (e: Exception) {
            logg.tag(executionKey).v("Processing, can't sync location history")
            return Result.failure(e)
        }

        try {
            triggerRemoteMaybe(
                remotePreference = RemotePreference.CACHE(
                    key = executionKey,
                    timeMillis = cacheTime.inWholeMilliseconds,
                    failOnError = false
                ),
                remote = { syncPendingEntriesImpl() }
            )

            return Result.success(Unit)
        } catch (e: Exception) {
            logg.tag(executionKey).w("Error syncing pending history entries - ${e.message}")
            return Result.failure(e)
        } finally {
            stopProcessing(executionKey)
        }
    }

    private suspend fun syncPendingEntriesImpl() {
        val pendingEntries = database.pendingLocationHistoryDao
            .getAllEntriesOnce()
            .let { historyMapper.pendingLocationToCore(it) }

        val personalHistory = pendingEntries.mapPersonalHistory()
        val groupHistory = groupLocalDataSource.allGroupsState.value.data?.mapNotNull {
            pendingEntries.mapGroupHistory(groupId = it.id)
        } ?: emptyList()

        logg.tag(executionKey).v("Found ${pendingEntries.size} pending history entries, personal (${personalHistory?.entries?.size} new, ${personalHistory?.retryEntries?.size} retries), groups (${groupHistory.sumOf { it.entries.size }} new, ${groupHistory.sumOf { it.retryEntries.size }} retries)")

        if (personalHistory == null && groupHistory.isEmpty()) {
            return
        }

        val payload = LocationHistoryUploadPayload(
            deviceId = appPrefs.deviceId,
            groupHistory = groupHistory,
            personalHistory = personalHistory
        )

        try {
            logg.tag(executionKey).v("Sending location history entry")
            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_CREATE,
                data = payload
            )

            if (response.isSuccess) {
                val response = response.response<HistoryUploadResponse>()

                response.data.personalHistoryResult?.let {
                    database.pendingLocationHistoryDao.deleteUnconfirmedUploadEntries(
                        ids = it.uploadedEntries
                    )

                    it.uploadedHistoryKeys.forEach {
                        database.pendingLocationHistoryDao.updateLocalHistoryKey(
                            groupId = UserGroup.groupPersonalDevicesId,
                            historyKeyVersion = it,
                            synced = true
                        )
                    }
                }

                response.data.groupHistoryResult?.let {
                    database.pendingLocationHistoryDao.deleteUnconfirmedUploadEntries(
                        ids = it.uploadedEntries
                    )
                }
            } else {
                throw response.remoteException
            }
        } catch (e: Exception) {
            logg.e("Error sending location history entry", e)
        }
    }

    private suspend fun List<LocationHistoryPointModelForMember>.mapPersonalHistory(): LocationHistoryPersonalPayload? {

        val personalHistoryNodes = this
            .filter { it.groupId == UserGroup.groupPersonalDevicesId }

        val result = if (personalHistoryNodes.isNotEmpty()) {
            val historyKeyEntity =
                database.pendingLocationHistoryDao.getLatestHistoryKey(groupOrSpaceId = UserGroup.groupPersonalDevicesId)
                    ?.takeIf { key -> key.createdAtInstant?.isOlderThan(30.days) == false }

            val historyKeyData = if (historyKeyEntity == null) {
                // create new key
                val newKey = cryptoCore.getNewSymmetricKeyForLocationData()
                val userKey = cryptoUserKeysRepo.personalKey()
                HistoryKeyPayload(
                    key = newKey,
                    encryptedKey = cryptoCore.encryptDataWithKey(
                        data = newKey,
                        key = userKey.key
                    ),
                    userKeyVersion = userKey.version,
                    historyKeyVersion = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED)
                )
            } else {
                HistoryKeyPayload(
                    key = cryptoCore.decryptDataWithKey(
                        data = historyKeyEntity.encryptedHistoryKey,
                        key = cryptoUserKeysRepo.personalKey(version = historyKeyEntity.userKeyVersion).key
                    ),
                    encryptedKey = historyKeyEntity.encryptedHistoryKey,
                    userKeyVersion = historyKeyEntity.userKeyVersion,
                    historyKeyVersion = historyKeyEntity.historyKeyVersion,
                )
            }

            val newKeyData = if (historyKeyEntity == null) {
                LocationHistoryPersonalKeyPayload(
                    historyKeyVersion = historyKeyData.historyKeyVersion,
                    userKeyVersion = historyKeyData.userKeyVersion,
                    encryptedHistoryKey = historyKeyData.encryptedKey
                )
            } else {
                null
            }

            newKeyData to personalHistoryNodes
                .groupBy { it.history.dayKey }
                .mapNotNull { entry ->
                    val dateKey = entry.key
                    if (!dateKey.isNullOrBlank()) {

                        val chunked = if (entry.value.size > 120) {
                            entry.value.chunked(100)
                        } else {
                            listOf(entry.value)
                        }

                        chunked.map { list ->
                            val model = LocationHistoryModel(
                                history = list.map { it.history }
                            )

                            LocationHistoryPayloadEntry(
                                dateIdentifier = dateKey,
                                historyData = cryptoCore.encryptDataWithKey(
                                    data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                                        model
                                    ),
                                    key = historyKeyData.key
                                ),
                                pendingUploadId = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED),
                                historyKeyVersion = historyKeyData.historyKeyVersion
                            )
                        }
                    } else {
                        null
                    }
                }
                .flatMap { it }
        } else {
            null to emptyList()
        }

        val newKeyData = result.first
        val personalHistory = result.second

        val currentUnconfirmed = database.pendingLocationHistoryDao.getAllUnconfirmedEntriesForGroupOnce(groupId = UserGroup.groupPersonalDevicesId)
            .map {
                LocationHistoryPayloadEntry(
                    dateIdentifier = it.dateIdentifier,
                    pendingUploadId = it.pendingUploadId,
                    historyData = it.locationHistoryData,
                    historyKeyVersion = it.historyKeyVersion
                )
            }

        val unsyncedKeys = database.pendingLocationHistoryDao.getUnsyncedHistoryKeys(groupId = UserGroup.groupPersonalDevicesId)
            .map {
                LocationHistoryPersonalKeyPayload(
                    historyKeyVersion = it.historyKeyVersion,
                    userKeyVersion = it.userKeyVersion,
                    encryptedHistoryKey = it.encryptedHistoryKey
                )
            }

        if (newKeyData != null) {
            database.pendingLocationHistoryDao.saveLocalHistoryKey(
                key = LocalLocationHistoryKeyEntity(
                    groupId = UserGroup.groupPersonalDevicesId,
                    createdAt = nowUTC().toString(),
                    userKeyVersion = newKeyData.userKeyVersion,
                    historyKeyVersion =  newKeyData.historyKeyVersion,
                    encryptedHistoryKey = newKeyData.encryptedHistoryKey,
                    isSynced = false
                )
            )
        }

        database.pendingLocationHistoryDao.createUnconfirmedEntries(
            entries = personalHistory.map {
                UnconfirmedLocationHistoryEntity(
                    pendingUploadId = it.pendingUploadId,
                    groupId = UserGroup.groupPersonalDevicesId,
                    dateIdentifier = it.dateIdentifier,
                    locationHistoryData = it.historyData,
                    historyKeyVersion = it.historyKeyVersion
                )
            }
        )

        database.pendingLocationHistoryDao.deletePendingEntries(
            ids = personalHistoryNodes.map { it.entryId }
        )

        if (personalHistory.isEmpty() && currentUnconfirmed.isEmpty() && unsyncedKeys.isEmpty() && newKeyData == null) {
            return null
        }

        return LocationHistoryPersonalPayload(
            entries = personalHistory,
            keys = listOfNotNull(newKeyData),
            retryEntries = currentUnconfirmed,
            retryKeys = unsyncedKeys
        )
    }

    private suspend fun List<LocationHistoryPointModelForMember>.mapGroupHistory(groupId: String): LocationHistoryGroupPayload? {

        val group = groupLocalDataSource.getGroupWithMembersOnce(groupId = groupId)

        if (group == null) {
            return null
        }

        val result = validityVerification.checkIntegrityForDeviceKey(group = group)
        if (result == IntegrityResult.CORRUPTED) {
            logg.tag(executionKey).v("Group device integrity is corrupted, clear pending location history for (${groupId})")
            database.pendingLocationHistoryDao.deleteAllPendingEntriesForGroup(groupOrSpaceId = groupId)
            database.pendingLocationHistoryDao.deleteAllUnconfirmedEntriesForGroup(groupId = groupId)
            return null
        }

        val historyKeys = database.pendingLocationHistoryDao.getHistoryKeys(groupId = groupId)

        val groupHistoryNodes = this.filter { it.groupId == groupId }

        val groupHistory = if (groupHistoryNodes.isNotEmpty()) {
            groupHistoryNodes
                .groupBy { it.history.dayKey }
                .mapValues { it.value.groupBy { entry -> entry.localHistoryKeyId } }
                .flatMap { entryPerDay ->
                    val dateKey = entryPerDay.key
                    if (!dateKey.isNullOrBlank()) {
                        entryPerDay.value.mapNotNull { entryPerKey ->

                            val historyKey = historyKeys.firstOrNull { it.id == entryPerKey.key }
                                ?.takeIf { it.historyKeyVersion.isNotBlank() }

                            if (historyKey != null) {
                                val historyKeyData = HistoryKeyPayload(
                                    key = cryptoCore.decryptDataWithKey(
                                        data = historyKey.encryptedHistoryKey,
                                        key = cryptoUserKeysRepo.personalKey(version = historyKey.userKeyVersion).key
                                    ),
                                    encryptedKey = historyKey.encryptedHistoryKey,
                                    userKeyVersion = historyKey.userKeyVersion,
                                    historyKeyVersion = historyKey.historyKeyVersion
                                )

                                val chunked = if (entryPerKey.value.size > 120) {
                                    entryPerKey.value.chunked(100)
                                } else {
                                    listOf(entryPerKey.value)
                                }

                                chunked.map { list ->
                                    val model = LocationHistoryModel(
                                        history = list.map { it.history }
                                    )

                                    LocationHistoryPayloadEntry(
                                        dateIdentifier = dateKey,
                                        historyData = cryptoCore.encryptDataWithKey(
                                            data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                                                model
                                            ),
                                            key = historyKeyData.key
                                        ),
                                        pendingUploadId = cryptoCore.randomId(length = 32, alphabet = AlphabetType.EXTENDED),
                                        historyKeyVersion = historyKey.historyKeyVersion
                                    )
                                }
                            } else {
                                null
                            }
                        }
                    } else {
                        listOf()
                    }
                }
                .flatMap { it }
        } else {
            emptyList()
        }

        val currentUnconfirmed = database.pendingLocationHistoryDao.getAllUnconfirmedEntriesForGroupOnce(groupId = groupId)
            .map {
                LocationHistoryPayloadEntry(
                    dateIdentifier = it.dateIdentifier,
                    pendingUploadId = it.pendingUploadId,
                    historyData = it.locationHistoryData,
                    historyKeyVersion = it.historyKeyVersion
                )
            }

        database.pendingLocationHistoryDao.createUnconfirmedEntries(
            entries = groupHistory.map {
                UnconfirmedLocationHistoryEntity(
                    pendingUploadId = it.pendingUploadId,
                    groupId = groupId,
                    dateIdentifier = it.dateIdentifier,
                    locationHistoryData = it.historyData,
                    historyKeyVersion = it.historyKeyVersion
                )
            }
        )

        database.pendingLocationHistoryDao.deletePendingEntries(
            ids = groupHistoryNodes.map { it.entryId }
        )

        if (groupHistory.isEmpty() && currentUnconfirmed.isEmpty()) {
            return null
        }

        return LocationHistoryGroupPayload(
            groupId = groupId,
            locationId = group.currentDeviceLocation?.id ?: "",
            entries = groupHistory,
            retryEntries = currentUnconfirmed
        )
    }

    private data class HistoryKeyPayload(
        val key: String,
        val encryptedKey: String,
        val userKeyVersion: Long,
        val historyKeyVersion: String,
    )
}