package app.paralino.core.domain.location.history

import app.paralino.core.db.AppPrefs
import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.raw.LocationHistoryDeletePeriodGroupPayload
import app.paralino.core.models.raw.LocationHistoryDeletePeriodPersonalPayload
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.utils.extensions.historyDateKeyForApi
import kotlinx.datetime.LocalDate

class DeleteLocationHistoryPeriodUseCase (
    private val database: MainDatabaseHelper,
    private val deletePendingLocation: DeletePendingLocationUseCase,
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

        deletePendingLocation.clearPendingAndCachedEntries(
            groupOrSpaceId = groupId,
            startDate = startDate,
            endDate = endDate,
            locationIds = locationIds,
            deleteForAllDevices = deleteForAllDevices
        )
    }
}