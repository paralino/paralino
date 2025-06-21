package app.paralino.core.domain.location.history

import app.paralino.core.crypto.CryptoCore
import app.paralino.core.crypto.LocationHistoryKey
import app.paralino.core.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModel
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.models.raw.LocationHistoryDeleteGroupPayload
import app.paralino.core.models.raw.LocationHistoryDeletePayloadEntry
import app.paralino.core.models.raw.LocationHistoryDeletePersonalPayload
import app.paralino.core.presentation.history.LocationHistoryPointState
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.BaseRemoteModule
import app.paralino.core.utils.exceptions.PermissionDeniedException
import kotlinx.datetime.LocalDate

class DeleteLocationHistoryEntryUseCase(
    private val database: MainDatabaseHelper,
    private val mapper: LocationHistoryMapper,
    private val functionsApi: IFunctionsApi,
    private val cryptoCore: CryptoCore,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val refreshLocationHistoryUseCase: RefreshLocationHistoryUseCase,
    private val appPrefs: AppPrefs
) {

    suspend fun deleteEntries(
        groupId: String,
        memberUserId: String,
        entries: List<LocationHistoryPointState>,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (memberUserId != appPrefs.userId) {
            throw PermissionDeniedException(code = 400, "Not authorized for action")
        }

        if (groupId == UserGroup.groupPersonalDevicesId) {
            deletePersonalHistoryEntry(entries)
        } else {
            deleteGroupHistoryEntry(groupId = groupId, memberUserId = memberUserId, entries = entries)
        }

        refreshLocationHistoryUseCase.refreshLocationHistory(
            groupId = groupId,
            memberUserId = memberUserId,
            locationId = null,
            startDate = startDate,
            endDate = endDate
        )
    }

    private suspend fun deletePersonalHistoryEntry(
        entries: List<LocationHistoryPointState>,
    ) {
        val cachedEntries = entries.filter { it.isCachedLocation }
        val syncedEntries = entries.filter { !it.isCachedLocation }

        val result = syncedEntries
            .groupBy { it.entryId }
            .mapNotNull { entry ->
                val databaseEntry = database.locationHistoryDao.getPersonalHistoryEntryOnce(id = entry.key)
                if (databaseEntry != null) {
                    val key = database.locationHistoryDao.getPersonalHistoryKeysOnce(
                        keys = listOf(databaseEntry.historyKeyVersion)
                    ).firstOrNull()?.let {
                        cryptoKEKRepo.parseLocationHistoryKey(keyRaw = it) as? LocationHistoryKey.Valid
                    }

                    if (key != null) {
                        val history = mapper.remote2Core(
                            groupId = UserGroup.groupPersonalDevicesId,
                            memberUserId = appPrefs.userId,
                            historyKey = key,
                            entry = databaseEntry.toReadModel()
                        )

                        val newHistory = history.history.mapSuccess {
                            it.history.filter { point -> entry.value.any { it.point.properID == point.properID } }
                        }.data

                        if (newHistory != null) {
                            LocationHistoryDeletePayloadEntry(
                                id = entry.key,
                                dateIdentifier = databaseEntry.dateIdentifier,
                                historyData = cryptoCore.encryptDataWithKey(
                                    data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                                        LocationHistoryModel(newHistory)
                                    ),
                                    key = key.key
                                ),
                                historyKeyVersion = key.historyKeyVersion
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
            }

        if (result.isEmpty()) {
            return
        }

        val payload = LocationHistoryDeletePersonalPayload(
            entries = result
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.HISTORY_UPDATE_PERSONAL,
            data = payload
        )

        if (!response.isSuccess) {
            throw response.remoteException
        }
    }

    private suspend fun deleteGroupHistoryEntry(
        groupId: String,
        memberUserId: String,
        entries: List<LocationHistoryPointState>,
    ) {
        // handle cached entries
        val cachedEntries = entries.filter { it.isCachedLocation }
        val syncedEntries = entries.filter { !it.isCachedLocation }
        val result = syncedEntries
            .groupBy { it.entryId }
            .mapNotNull { entry ->
                val databaseEntry = database.locationHistoryDao.getGroupHistoryEntryOnce(id = entry.key)
                if (databaseEntry != null) {
                    val key = database.locationHistoryDao.getGroupHistoryKeysOnce(
                        groupId = groupId,
                        ownerUserId = memberUserId,
                        keys = listOf(databaseEntry.historyKeyVersion)
                    ).firstOrNull()?.let {
                        cryptoKEKRepo.parseLocationHistoryKey(keyRaw = it) as? LocationHistoryKey.Valid
                    }

                    if (key != null) {
                        val history = mapper.remote2Core(
                            groupId = groupId,
                            memberUserId = memberUserId,
                            historyKey = key,
                            entry = databaseEntry.toReadModel()
                        )

                        val newHistory = history.history.mapSuccess {
                            it.history.filter { point -> entry.value.none { it.point.properID == point.properID } }
                        }.data

                        if (newHistory != null) {
                            LocationHistoryDeletePayloadEntry(
                                id = entry.key,
                                dateIdentifier = databaseEntry.dateIdentifier,
                                historyData = cryptoCore.encryptDataWithKey(
                                    data = BaseRemoteModule.locationHistoryFormatter.encodeToString(
                                        LocationHistoryModel(newHistory)
                                    ),
                                    key = key.key
                                ),
                                historyKeyVersion = key.historyKeyVersion
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
            }

        if (result.isEmpty()) {
            return
        }

        val payload = LocationHistoryDeleteGroupPayload(
            groupId = groupId,
            entries = result
        )

        val response = functionsApi.runAndWait(
            function = ServerFunction.HISTORY_UPDATE_GROUP,
            data = payload
        )

        if (!response.isSuccess) {
            throw response.remoteException
        }
    }
}