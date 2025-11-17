package app.paralino.core.domain.location.history

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.logg
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.user.UserPersonalSpaceLocalDataSource
import app.paralino.core.utils.extensions.historyDateKeyForApi
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class DeleteObsoleteLocationHistoryUseCase(
    private val database: MainDatabaseHelper,
    private val userGroupLocalDataSource: UserGroupLocalDataSource,
    private val userPersonalSpaceLocalDataSource: UserPersonalSpaceLocalDataSource,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val deletePendingLocation: DeletePendingLocationUseCase,
) {

    suspend fun deleteObsoleteHistory() {
        try {
            val today = nowUTC().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val config = remoteConfigRepository.getConfigOnce()

            // Delete obsolete personal space history
            userPersonalSpaceLocalDataSource.personalSpaceState.value.data?.let { personalSpace ->
                val planConfig = personalSpace.plan
                
                // Only delete if history is not unlimited and is enabled
                if (!planConfig.hasUnlimitedPersonalHistory) {
                    val cutoffDate = today.minus(DatePeriod(days = planConfig.personalHistoryDaysLimit + 2))
                    val cutoffDateKey = cutoffDate.historyDateKeyForApi()
                    
                    logg.tag("delete_obsolete_history").d("Deleting personal history older than $cutoffDateKey (limit: ${planConfig.personalHistoryDaysLimit} days)")
                    
                    database.locationHistoryDao.deletePersonalHistoryByDateRange(
                        locationIds = emptyList(), // not used when deleteForAllDevices is true
                        deleteForAllDevices = true,
                        startDate = null,
                        endDate = cutoffDateKey
                    )

                    deletePendingLocation.clearPendingAndCachedEntries(
                        groupOrSpaceId = UserGroup.groupPersonalDevicesId.asGroupOrSpaceId(),
                        startDate = null,
                        endDate = cutoffDate,
                        locationIds = emptyList(),
                        deleteForAllDevices = true,
                    )
                }

                database.personalLocalHistoryKeyDao.clearUnusedSyncedKeys()
            }

            // Delete obsolete group history
            userGroupLocalDataSource.allGroupsState.value.data?.forEach { groupMetadata ->
                val group = groupMetadata.group
                val planConfig = group.planConfig(config)
                
                // Only delete if history is not unlimited and is enabled
                if (!planConfig.hasUnlimitedGroupHistory) {
                    val cutoffDate = today.minus(DatePeriod(days = planConfig.historyDaysLimit + 2))
                    val cutoffDateKey = cutoffDate.historyDateKeyForApi()
                    
                    logg.tag("delete_obsolete_history").d("Deleting group ${group.id} history older than $cutoffDateKey (limit: ${planConfig.historyDaysLimit} days)")
                    
                    database.locationHistoryDao.deleteGroupHistoryByDateRange(
                        groupId = group.id,
                        memberUserId = null, // deleting for all members
                        locationIds = emptyList(), // not used when deleteForAllDevices is true
                        deleteForAllDevices = true,
                        startDate = null,
                        endDate = cutoffDateKey
                    )

                    deletePendingLocation.clearPendingAndCachedEntries(
                        groupOrSpaceId = group.id.asGroupOrSpaceId(),
                        startDate = null,
                        endDate = cutoffDate,
                        locationIds = emptyList(),
                        deleteForAllDevices = true,
                    )
                }

                database.groupLocalHistoryKeyDao.deleteAllInactiveKeys(groupId = group.id)
            }
            
            logg.tag("delete_obsolete_history").d("Obsolete history cleanup completed")
        } catch (e: Exception) {
            logg.tag("delete_obsolete_history").e("Error deleting obsolete history", e)
        }
    }
}

