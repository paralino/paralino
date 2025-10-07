package app.paralino.core.domain.location.history

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationHistoryModelForUser
import app.paralino.core.models.mappers.LocationHistoryMapper
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.utils.extensions.dateForApi
import app.paralino.core.utils.extensions.listOfDatesUntil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.plus

class GetLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val historyMapper: LocationHistoryMapper
) : BaseRepository() {

    fun getLocationHistory(
        groupId: String,
        memberUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<LocationHistoryModelForUser> {
        if (groupId == UserGroup.Companion.groupPersonalDevicesId) {
            return getPersonalLocationHistory(
                startDate = startDate,
                endDate = endDate
            )
        } else {
            return getLocationHistoryForMember(
                groupId = groupId,
                memberUserId = memberUserId,
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    private fun getLocationHistoryForMember(
        groupId: String,
        memberUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<LocationHistoryModelForUser> {

        val dateList = startDate.listOfDatesUntil(endDate).map { it.dateForApi() }

        return database.locationHistoryDao.getGroupHistory(
            groupId = groupId,
            ownerUserId = memberUserId,
            dates = dateList
        ).map {
            val keys = database.locationHistoryDao.getGroupHistoryKeysOnce(
                groupId = groupId,
                ownerUserId = memberUserId,
                keys = it.map { it.historyKeyVersion }.distinct()
            )

            historyMapper.local2CoreGroup(
                groupId = groupId,
                memberUserId = memberUserId,
                keys = keys,
                entries = it
            )
        }
    }

    private fun getPersonalLocationHistory(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<LocationHistoryModelForUser> {

        val dateList = startDate.listOfDatesUntil(endDate).map { it.dateForApi() }

        return database.locationHistoryDao.getPersonalHistory(
            dates = dateList
        ).map {
            val keys = database.locationHistoryDao.getPersonalHistoryKeysOnce(
                keys = it.map { it.historyKeyVersion }.distinct()
            )

            historyMapper.local2CorePersonal(
                keys = keys,
                entries = it
            )
        }
    }
}