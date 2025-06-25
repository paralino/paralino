package app.paralino.core.domain.location.history

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.models.raw.GroupLocationHistoryReadPayload
import app.paralino.core.models.raw.GroupLocationHistoryReadResponse
import app.paralino.core.models.raw.PersonalLocationHistoryReadPayload
import app.paralino.core.models.raw.PersonalLocationHistoryReadResponse
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.dateForApi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern

class RefreshLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val mapper: LocationHistoryMapper,
    private val functionsApi: IFunctionsApi,
): BaseRepository() {

    suspend fun refreshLocationHistory(
        groupId: String,
        memberUserId: String,
        locationId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        if (groupId == UserGroup.groupPersonalDevicesId) {
            refreshPersonalLocationHistory(
                locationId = locationId,
                startDate = startDate,
                endDate = endDate
            )
        } else {
            refreshGroupLocationHistory(
                groupId = groupId,
                memberUserId = memberUserId,
                locationId = locationId,
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    private suspend fun refreshGroupLocationHistory(
        groupId: String,
        memberUserId: String,
        locationId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
    ): GroupLocationHistoryReadResponse {
        return triggerRemoteOnly {
            val payload = GroupLocationHistoryReadPayload(
                groupId = groupId,
                memberUserId = memberUserId,
                locationId = locationId,
                startDateKey = startDate.dateForApi(),
                endDateKey = endDate.dateForApi()
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_READ_GROUP,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            return@triggerRemoteOnly response.response<GroupLocationHistoryReadResponse>().data
        }.also { response ->
            val keys = response.keys.map {
                mapper.remote2LocalGroup(groupId = groupId, memberUserId = memberUserId, model = it)
            }

            val entries = response.entries.map {
                mapper.remote2LocalGroup(groupId = groupId, memberUserId = memberUserId, model = it)
            }

            database.locationHistoryDao.recreateGroupHistory(
                dates = response.dateKeys,
                groupId = groupId,
                memberUserId = memberUserId,
                entries = entries,
                keys = keys
            )
        }
    }

    private suspend fun refreshPersonalLocationHistory(
        locationId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
    ): PersonalLocationHistoryReadResponse {
        return triggerRemoteOnly {
            val payload = PersonalLocationHistoryReadPayload(
                locationId = locationId,
                startDateKey = startDate.dateForApi(),
                endDateKey = endDate.dateForApi()
            )

            val response = functionsApi.runAndWait(
                function = ServerFunction.HISTORY_READ_PERSONAL,
                data = payload
            )

            if (!response.isSuccess) {
                throw response.remoteException
            }

            return@triggerRemoteOnly response.response<PersonalLocationHistoryReadResponse>().data
        }.also { response ->
            val keys = response.keys.map {
                mapper.remote2LocalPersonal(model = it)
            }

            val entries = response.entries.map {
                mapper.remote2LocalPersonal(model = it)
            }

            database.locationHistoryDao.createPersonalHistory(entries = entries)
            database.locationHistoryDao.createPersonalKeys(entries = keys)
        }
    }
}