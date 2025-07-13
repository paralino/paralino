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
import app.paralino.core.repo.base.RemotePreference
import app.paralino.core.utils.extensions.dateForApi
import app.paralino.core.utils.extensions.listOfDatesUntil
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class RefreshLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val mapper: LocationHistoryMapper,
    private val functionsApi: IFunctionsApi,
): BaseRepository() {

    private val mutex = Mutex()

    suspend fun refreshLocationHistory(
        groupId: String,
        memberUserId: String,
        locationId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        requestCacheDuration: Duration
    ) {
        mutex.withLock {
            if (groupId == UserGroup.groupPersonalDevicesId) {
                refreshPersonalLocationHistory(
                    locationId = locationId,
                    startDate = startDate,
                    endDate = endDate,
                    requestCacheDuration = requestCacheDuration
                )
            } else {
                refreshGroupLocationHistory(
                    groupId = groupId,
                    memberUserId = memberUserId,
                    locationId = locationId,
                    startDate = startDate,
                    endDate = endDate,
                    requestCacheDuration = requestCacheDuration
                )
            }
        }
    }

    private suspend fun refreshGroupLocationHistory(
        groupId: String,
        memberUserId: String,
        locationId: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        requestCacheDuration: Duration
    ): GroupLocationHistoryReadResponse? {
        val key = startDate.listOfDatesUntil(endDate).joinToString { it.dateForApi() }
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "${groupId}_${memberUserId}_${key}",
                timeMillis = requestCacheDuration.inWholeMilliseconds
            )
        ) {
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

            return@triggerRemoteMaybe response.response<GroupLocationHistoryReadResponse>().data
        }?.also { response ->
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
        requestCacheDuration: Duration
    ): PersonalLocationHistoryReadResponse? {
        val key = startDate.listOfDatesUntil(endDate).joinToString { it.dateForApi() }
        return triggerRemoteMaybe(
            remotePreference = RemotePreference.CACHE(
                key = "personal_history_${key}",
                timeMillis = requestCacheDuration.inWholeMilliseconds
            )
        ) {
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

            return@triggerRemoteMaybe response.response<PersonalLocationHistoryReadResponse>().data
        }?.also { response ->
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