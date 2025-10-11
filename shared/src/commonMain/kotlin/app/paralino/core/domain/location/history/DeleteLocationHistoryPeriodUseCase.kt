package app.paralino.core.domain.location.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.location.SaveLocationUseCase
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.models.raw.LocationHistoryDeletePeriodGroupPayload
import app.paralino.core.models.raw.LocationHistoryDeletePeriodPersonalPayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.extensions.historyDateKeyForApi
import app.paralino.core.utils.extensions.historyDateKeyFromApi
import kotlinx.datetime.LocalDate

class DeleteLocationHistoryPeriodUseCase (
    private val database: MainDatabaseHelper,
    private val pendingHistoryMapper: PendingLocationHistoryMapper,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val functionsApi: IFunctionsApi,
    private val appPrefs: AppPrefs
) {

    suspend fun deleteHistory(
        groupId: GroupOrSpaceId,
        locationIds: List<String>,
        deleteForAllDevices: Boolean,
        startDate: LocalDate?,
        endDate: LocalDate?
    ) {
        if (startDate == null && endDate == null) {
            throw IllegalArgumentException("Please set at least one date")
        }

        val startDateKey = startDate?.historyDateKeyForApi()
        val endDateKey = endDate?.historyDateKeyForApi()

        if (groupId.isPersonalSpace) {
            val payload = LocationHistoryDeletePeriodPersonalPayload(
                startDateKey = startDateKey,
                endDateKey = endDateKey,
                locationIds = locationIds,
                deleteForAllDevices = deleteForAllDevices
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_DELETE_PERSONAL,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            database.locationHistoryDao.deletePersonalHistoryByDateRange(
                startDate = startDateKey,
                endDate = endDateKey,
                locationIds = locationIds,
                deleteForAllDevices = deleteForAllDevices
            )
        } else {
            val payload = LocationHistoryDeletePeriodGroupPayload(
                groupId = groupId.id,
                startDateKey = startDateKey,
                endDateKey = endDateKey,
                locationIds = locationIds,
                deleteForAllDevices = deleteForAllDevices
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_DELETE_GROUP,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            database.locationHistoryDao.deleteGroupHistoryByDateRange(
                groupId = groupId.id,
                memberUserId = appPrefs.userId,
                startDate = startDateKey,
                endDate = endDateKey,
                locationIds = locationIds,
                deleteForAllDevices = deleteForAllDevices
            )
        }

        clearCachedEntries(
            groupOrSpaceId = groupId,
            startDate = startDate,
            endDate = endDate,
            locationIds = locationIds,
            deleteForAllDevices = deleteForAllDevices
        )
    }

    private suspend fun clearCachedEntries(
        groupOrSpaceId: GroupOrSpaceId,
        startDate: LocalDate?,
        endDate: LocalDate?,
        locationIds: List<String>,
        deleteForAllDevices: Boolean,
    ) {
        val toDeletePending = database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = groupOrSpaceId.id)
            .let { pendingHistoryMapper.pendingLocationToCore(it) }
            .filter { pending ->
                val dateCheck = pending.history.dayKey?.historyDateKeyFromApi()?.let { date ->
                    if (startDate != null && endDate != null) {
                        date >= startDate && date <= endDate
                    } else if (startDate != null) {
                        date >= startDate
                    } else if (endDate != null) {
                        date <= endDate
                    } else {
                        false
                    }
                } ?: false

                val locationCheck = if (deleteForAllDevices) {
                    true
                } else {
                    locationIds.any { it == pending.locationId }
                }

                dateCheck && locationCheck
            }
            .map { it.entryId }

        if (toDeletePending.isNotEmpty()) {
            database.pendingLocationHistoryDao.deletePendingEntries(ids = toDeletePending)
            locationIds.forEach {
                saveLocationUseCase.uploadAndSaveCurrentLocation(
                    data = MemberGroupIdForUpload(
                        groupId = groupOrSpaceId,
                        locationId = it,
                        locationData = null,
                        deviceStatus = null,
                        uploadPendingHistory = true
                    )
                )
            }
        }
    }
}