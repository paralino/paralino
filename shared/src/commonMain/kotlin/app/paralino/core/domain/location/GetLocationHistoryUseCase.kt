package app.paralino.core.domain.location

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.location.GroupHistoryEntity
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.models.raw.GroupLocationHistoryReadPayload
import app.paralino.core.models.raw.GroupLocationHistoryReadResponse
import app.paralino.core.repo.api.IFunctionsApi
import app.paralino.core.repo.api.ServerFunction
import app.paralino.core.repo.api.runAndWait
import app.paralino.core.repo.base.BaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class GetLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val functionsApi: IFunctionsApi,
    private val historyMapper: LocationHistoryMapper
): BaseRepository() {

    fun getLocationHistoryForMember(
        groupId: String,
        memberUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<LocationHistoryModelForUser> {

        var currentDate = startDate
        val dateList = mutableListOf<String>()
        while (currentDate <= endDate) {
            dateList.add(currentDate.toString())
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        return database.locationHistoryDao.getGroupHistory(
            groupId = groupId,
            ownerUserId = memberUserId,
            dates = dateList
        ).map {
            val keys = database.locationHistoryDao.getGroupHistoryKeysOnce(
                groupId = groupId,
                ownerUserId = memberUserId,
                keys = it.map { it.historyKeyVersion }
            )

            historyMapper.local2Core(
                groupId = groupId,
                memberUserId = memberUserId,
                keys = keys,
                entries = it
            )
        }
    }


    suspend fun refreshLocationHistory(
        groupId: String,
        memberUserId: String,
        locationId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): GroupLocationHistoryReadResponse {
        return triggerRemoteOnly {
            val payload = GroupLocationHistoryReadPayload(
                groupId = groupId,
                memberUserId = memberUserId,
                locationId = locationId,
                startDateKey = startDate.toString(),
                endDateKey = endDate.toString()
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
                GroupHistoryKeyEntity(
                    id = groupId + memberUserId + it.keyVersion,
                    groupId = groupId,
                    ownerUserId = memberUserId,
                    memberKeyVersion = it.memberKeyVersion,
                    historyKeyVersion = it.keyVersion,
                    encryptedHistoryKey = it.encryptedKey
                )
            }

            val entries = response.entries.map {
                GroupHistoryEntity(
                    id = it.id,
                    dateIdentifier = it.dateKey,
                    groupId = groupId,
                    ownerUserId = memberUserId,
                    locationId = locationId,
                    deviceId = it.deviceId,
                    historyKeyVersion = it.keyVersion,
                    historyData = it.historyData
                )
            }

            database.locationHistoryDao.createGroupHistory(entries = entries)
            database.locationHistoryDao.createGroupKeys(entries = keys)
        }
    }
}