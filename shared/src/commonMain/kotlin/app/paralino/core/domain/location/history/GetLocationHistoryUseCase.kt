package app.paralino.core.domain.location.history

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.db.main.location.GroupHistoryEntity
import app.paralino.core.db.main.location.PersonalHistoryEntity
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationHistoryModelForDevice
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.location.LocationHistoryPointId
import app.paralino.core.models.mapSuccess
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.models.raw.LocationHistoryReadEntry
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.dateForApi
import app.paralino.core.utils.extensions.listOfDatesUntil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class GetLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val historyMapper: LocationHistoryMapper
) : BaseRepository() {

    fun getLocationHistory(
        groupOrSpaceId: GroupOrSpaceId,
        memberUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<LocationHistoryModelForUser> {
        val dateList = startDate.listOfDatesUntil(endDate).map { it.dateForApi() }
        if (groupOrSpaceId.isPersonalSpace) {
            return database.locationHistoryDao.getPersonalHistory(dates = dateList)
                .map { mapPersonalLocationHistory(history = it) }
        } else {
            return database.locationHistoryDao.getGroupHistory(
                groupId = groupOrSpaceId.id,
                ownerUserId = memberUserId,
                dates = dateList
            ).map {
                mapLocationHistoryForGroupMember(
                    groupId = groupOrSpaceId.id,
                    memberUserId = memberUserId,
                    history = it
                )
            }
        }
    }

    fun getLocationHistoryRawPresentationByIds(
        groupOrSpaceId: GroupOrSpaceId,
        memberUserId: String,
        pointIds: List<LocationHistoryPointId>
    ): Flow<List<Pair<LocationHistoryReadEntry, LocationHistoryModelForDevice?>>> {
        if (groupOrSpaceId.isPersonalSpace) {
            return database.locationHistoryDao.getPersonalHistoryEntriesByIds(
                ids = pointIds.map { it.entryId }.distinct()
            )
                .map { history ->
                    val mappedHistory = mapPersonalLocationHistory(history = history)
                    history.map { entry ->
                        val matchingPoint = mappedHistory.history.firstOrNull { it.entryId == entry.id }
                        val points = matchingPoint?.copy(
                            history = matchingPoint.history.mapSuccess {
                                it.copy(history = it.history.filter { point -> pointIds.any { it.pointId == point.properID } })
                            }
                        )
                        historyMapper.local2RemotePersonal(entry) to points
                    }
                }
        } else {
            return database.locationHistoryDao.getGroupHistoryEntriesByIds(
                groupId = groupOrSpaceId.id,
                ownerUserId = memberUserId,
                ids = pointIds.map { it.entryId }.distinct()
            ).map { history ->
                val mappedHistory = mapLocationHistoryForGroupMember(
                    groupId = groupOrSpaceId.id,
                    memberUserId = memberUserId,
                    history = history
                )
                history.map { entry ->
                    val matchingPoint = mappedHistory.history.firstOrNull { it.entryId == entry.id }
                    val points = matchingPoint?.copy(
                        history = matchingPoint.history.mapSuccess {
                            it.copy(history = it.history.filter { point -> pointIds.any { it.pointId == point.properID } })
                        }
                    )
                    historyMapper.local2RemoteGroup(entry) to points
                }
            }
        }
    }

    private suspend fun mapLocationHistoryForGroupMember(
        groupId: String,
        memberUserId: String,
        history: List<GroupHistoryEntity>
    ): LocationHistoryModelForUser {
        val keys = database.locationHistoryDao.getGroupHistoryKeysOnce(
            groupId = groupId,
            ownerUserId = memberUserId,
            keys = history.map { it.historyKeyVersion }.distinct()
        )

        return historyMapper.local2CoreGroup(
            groupId = groupId,
            memberUserId = memberUserId,
            keys = keys,
            entries = history
        )
    }

    private suspend fun mapPersonalLocationHistory(
        history: List<PersonalHistoryEntity>
    ): LocationHistoryModelForUser {
        val keys = database.locationHistoryDao.getPersonalHistoryKeysOnce(
            keys = history.map { it.historyKeyVersion }.distinct()
        )

        return historyMapper.local2CorePersonal(
            keys = keys,
            entries = history
        )
    }
}