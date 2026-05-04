package app.paralino.core.domain.location.history

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.domain.location.SaveLocationUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.models.location.MemberGroupIdForUpload
import app.paralino.core.models.mappers.PendingLocationHistoryMapper
import app.paralino.core.utils.extensions.historyDateKeyFromApi
import kotlinx.datetime.LocalDate

class DeletePendingLocationUseCase(
    private val database: MainDatabaseHelper,
    private val pendingHistoryMapper: PendingLocationHistoryMapper,
    private val saveLocationUseCase: SaveLocationUseCase,
) {
    suspend fun clearPendingAndCachedEntries(
        groupOrSpaceId: GroupOrSpaceId,
        entries: List<LocationHistoryPointState.Point>,
    ) {
        val toDeletePending = database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = groupOrSpaceId.id)
            .let { pendingHistoryMapper.pendingLocationToCore(it) }
            .filter { pending ->
                entries.any { it.point.properID == pending.history.properID }
            }
            .map { it.entryId }

        if (toDeletePending.isNotEmpty()) {
            database.pendingLocationHistoryDao.deletePendingEntries(ids = toDeletePending)
            entries.mapNotNull { it.locationId }.distinct().forEach {
                try {
                    saveLocationUseCase.uploadAndSaveCurrentLocation(
                        data = MemberGroupIdForUpload(
                            groupId = groupOrSpaceId,
                            locationId = it,
                            locationData = null,
                            deviceStatus = null,
                            uploadPendingHistory = true
                        )
                    )
                } catch (e: Exception) {
                    logg.w("Error updating device location after history deletion", e)
                }
            }
        }
    }

    suspend fun clearPendingAndCachedEntries(
        groupOrSpaceId: GroupOrSpaceId,
        startDate: LocalDate?,
        endDate: LocalDate?,
        locationIds: List<String>,
        deleteForAllDevices: Boolean,
    ) {
        val toDeletePending = database.pendingLocationHistoryDao.getAllEntriesForGroupOnce(groupOrSpaceId = groupOrSpaceId.id)
            .filter { pending ->
                val dateCheck = pending.dateIdentifier.historyDateKeyFromApi()?.let { date ->
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
            .map { it.id }

        // TODO handle saveLocationUseCase when deleteForAllDevices is true
        // fetch all current location ids and update for them

        if (toDeletePending.isNotEmpty()) {
            database.pendingLocationHistoryDao.deletePendingEntries(ids = toDeletePending)
            locationIds.forEach {
                try {
                    saveLocationUseCase.uploadAndSaveCurrentLocation(
                        data = MemberGroupIdForUpload(
                            groupId = groupOrSpaceId,
                            locationId = it,
                            locationData = null,
                            deviceStatus = null,
                            uploadPendingHistory = true
                        )
                    )
                } catch (e: Exception) {
                    logg.w("Error updating device location after history deletion", e)
                }
            }
        }
    }
}