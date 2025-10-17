package app.paralino.core.db.main.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalGroupKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationHistoryDao {

    @Transaction
    suspend fun recreatePersonalHistory(
        dates: List<String>,
        entries: List<PersonalHistoryEntity>,
        historyKeys: List<PersonalHistoryKeyEntity>,
    ) {
        deleteOldPersonalHistory(dates = dates)
        deleteUnusedPersonalHistoryKeys(
            usedIds = getAllPersonalHistoryKeysVersions()
        )
        createPersonalHistory(entries = entries)
        createPersonalKeys(keys = historyKeys)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalKeys(keys: List<PersonalHistoryKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalHistory(entries: List<PersonalHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupKeys(keys: List<GroupHistoryKeyEntity>)


    @Transaction
    suspend fun recreateGroupHistory(
        groupId: String,
        memberUserId: String,
        dates: List<String>,
        entries: List<GroupHistoryEntity>,
        historyKeys: List<GroupHistoryKeyEntity>,
        kxKeys: List<PersonalGroupKeyEntity>,
    ) {
        deleteOldGroupHistory(groupId = groupId, memberUserId = memberUserId, dates = dates)
        deleteUnusedGroupHistoryKeys(
            groupId = groupId,
            memberUserId = memberUserId,
            usedIds = getAllGroupHistoryKeysVersions(groupId = groupId, memberUserId = memberUserId)
        )
        deleteUnusedArchivedPersonalGroupKeys(
            groupId = groupId,
            usedIds = getAllUsedPersonalGroupKeysVersions(groupId = groupId)
        )

        createGroupHistory(entries = entries)
        createGroupKeys(keys = historyKeys)
        createPersonalGroupKeys(keys = kxKeys)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupHistory(entries: List<GroupHistoryEntity>)


    @Query("SELECT historyKeyVersion FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId)")
    suspend fun getAllGroupHistoryKeysVersions(groupId: String, memberUserId: String?): List<String>

    @Query("DELETE FROM groupHistoryKeysEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND historyKeyVersion NOT IN (:usedIds)")
    suspend fun deleteUnusedGroupHistoryKeys(groupId: String, memberUserId: String?, usedIds: List<String>)



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalGroupKeys(keys: List<PersonalGroupKeyEntity>)

    @Query("DELETE FROM personalGroupKeyEntity WHERE groupId = :groupId AND isArchived = 1 AND personalGroupKeyVersion NOT IN (:usedIds)")
    suspend fun deleteUnusedArchivedPersonalGroupKeys(groupId: String, usedIds: List<Long>)

    @Query("SELECT memberKeyVersion FROM groupHistoryKeysEntity WHERE groupId = :groupId")
    suspend fun getAllUsedPersonalGroupKeysVersions(groupId: String): List<Long>




    @Query("SELECT * FROM groupHistoryEntity WHERE groupId = :groupId AND ownerUserId = :ownerUserId AND dateIdentifier IN (:dates)")
    fun getGroupHistory(
        groupId: String,
        ownerUserId: String,
        dates: List<String>
    ): Flow<List<GroupHistoryEntity>>

    @Query("SELECT * FROM groupHistoryEntity WHERE groupId = :groupId AND ownerUserId = :ownerUserId AND dateIdentifier IN (:dates)")
    suspend fun getGroupHistoryOnce(
        groupId: String,
        ownerUserId: String,
        dates: List<String>
    ): List<GroupHistoryEntity>

    @Query("SELECT * FROM groupHistoryEntity WHERE groupId = :groupId AND id = :id")
    suspend fun getGroupHistoryEntryOnce(groupId: String, id: String): GroupHistoryEntity?

    @Query("SELECT * FROM groupHistoryEntity WHERE groupId = :groupId AND ownerUserId = :ownerUserId AND id IN (:ids)")
    fun getGroupHistoryEntriesByIds(groupId: String, ownerUserId: String, ids: List<String>): Flow<List<GroupHistoryEntity>>

    @Query("SELECT * FROM groupHistoryKeysEntity WHERE groupId = :groupId AND ownerUserId = :ownerUserId AND historyKeyVersion IN (:keys)")
    suspend fun getGroupHistoryKeysOnce(
        groupId: String,
        ownerUserId: String,
        keys: List<String>
    ): List<GroupHistoryKeyEntity>


    @Query("SELECT * FROM personalHistoryEntity WHERE dateIdentifier IN (:dates)")
    fun getPersonalHistory(
        dates: List<String>
    ): Flow<List<PersonalHistoryEntity>>

    @Query("SELECT * FROM personalHistoryEntity WHERE dateIdentifier IN (:dates)")
    suspend fun getPersonalHistoryOnce(
        dates: List<String>
    ): List<PersonalHistoryEntity>

    @Query("SELECT * FROM personalHistoryEntity WHERE id = :id")
    suspend fun getPersonalHistoryEntryOnce(id: String): PersonalHistoryEntity?

    @Query("SELECT * FROM personalHistoryEntity WHERE id IN (:ids)")
    fun getPersonalHistoryEntriesByIds(ids: List<String>): Flow<List<PersonalHistoryEntity>>

    @Query("SELECT * FROM personalHistoryKeysEntity WHERE historyKeyVersion IN (:keys)")
    suspend fun getPersonalHistoryKeysOnce(
        keys: List<String>
    ): List<PersonalHistoryKeyEntity>

    @Query("SELECT historyKeyVersion FROM personalHistoryEntity")
    suspend fun getAllPersonalHistoryKeysVersions(): List<String>

    @Query("DELETE FROM personalHistoryKeysEntity WHERE historyKeyVersion NOT IN (:usedIds)")
    suspend fun deleteUnusedPersonalHistoryKeys(usedIds: List<String>)


    @Query("DELETE FROM personalHistoryEntity WHERE dateIdentifier IN (:dates)")
    suspend fun deleteOldPersonalHistory(dates: List<String>)

    @Query("DELETE FROM personalHistoryEntity WHERE dateIdentifier BETWEEN :startDate AND :endDate")
    suspend fun deletePersonalHistoryBetweenDates(startDate: String, endDate: String)

    @Query("DELETE FROM personalHistoryEntity WHERE dateIdentifier >= :startDate")
    suspend fun deletePersonalHistoryFromDate(startDate: String)

    @Query("DELETE FROM personalHistoryEntity WHERE dateIdentifier <= :endDate")
    suspend fun deletePersonalHistoryUntilDate(endDate: String)

    @Query("DELETE FROM personalHistoryEntity WHERE locationId IN (:locationIds) AND dateIdentifier BETWEEN :startDate AND :endDate")
    suspend fun deletePersonalHistoryByIdsAndBetweenDates(locationIds: List<String>, startDate: String, endDate: String)

    @Query("DELETE FROM personalHistoryEntity WHERE locationId IN (:locationIds) AND dateIdentifier >= :startDate")
    suspend fun deletePersonalHistoryByIdsAndFromDate(locationIds: List<String>, startDate: String)

    @Query("DELETE FROM personalHistoryEntity WHERE locationId IN (:locationIds) AND dateIdentifier <= :endDate")
    suspend fun deletePersonalHistoryByIdsAndUntilDate(locationIds: List<String>, endDate: String)

    // Wrapper function to handle nullable date parameters
    suspend fun deletePersonalHistoryByDateRange(
        locationIds: List<String>,
        deleteForAllDevices: Boolean,
        startDate: String? = null,
        endDate: String? = null
    ) {
        if (deleteForAllDevices) {
            when {
                startDate != null && endDate != null -> deletePersonalHistoryBetweenDates(startDate, endDate)
                startDate != null -> deletePersonalHistoryFromDate(startDate)
                endDate != null -> deletePersonalHistoryUntilDate(endDate)
            }
        } else {
            when {
                startDate != null && endDate != null -> deletePersonalHistoryByIdsAndBetweenDates(locationIds, startDate, endDate)
                startDate != null -> deletePersonalHistoryByIdsAndFromDate(locationIds, startDate)
                endDate != null -> deletePersonalHistoryByIdsAndUntilDate(locationIds, endDate)
            }
        }

        deleteUnusedPersonalHistoryKeys(
            usedIds = getAllPersonalHistoryKeysVersions()
        )
    }



    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND dateIdentifier IN (:dates)")
    suspend fun deleteOldGroupHistory(groupId: String, memberUserId: String?, dates: List<String>)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND dateIdentifier BETWEEN :startDate AND :endDate")
    suspend fun deleteGroupHistoryBetweenDates(groupId: String, memberUserId: String?, startDate: String, endDate: String)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND dateIdentifier >= :startDate")
    suspend fun deleteGroupHistoryFromDate(groupId: String, memberUserId: String?, startDate: String)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND dateIdentifier <= :endDate")
    suspend fun deleteGroupHistoryUntilDate(groupId: String, memberUserId: String?, endDate: String)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND locationId IN (:locationIds) AND dateIdentifier BETWEEN :startDate AND :endDate")
    suspend fun deleteGroupHistoryByIdsAndBetweenDates(groupId: String, memberUserId: String?, locationIds: List<String>, startDate: String, endDate: String)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND locationId IN (:locationIds) AND dateIdentifier >= :startDate")
    suspend fun deleteGroupHistoryByIdsAndFromDate(groupId: String, memberUserId: String?, locationIds: List<String>, startDate: String)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND (:memberUserId IS NULL OR ownerUserId = :memberUserId) AND locationId IN (:locationIds) AND dateIdentifier <= :endDate")
    suspend fun deleteGroupHistoryByIdsAndUntilDate(groupId: String, memberUserId: String?, locationIds: List<String>, endDate: String)

    // Wrapper function to handle nullable date parameters for group history
    suspend fun deleteGroupHistoryByDateRange(
        groupId: String,
        memberUserId: String?,
        locationIds: List<String>,
        deleteForAllDevices: Boolean,
        startDate: String? = null,
        endDate: String? = null
    ) {
        if (deleteForAllDevices) {
            when {
                startDate != null && endDate != null -> deleteGroupHistoryBetweenDates(groupId, memberUserId, startDate, endDate)
                startDate != null -> deleteGroupHistoryFromDate(groupId, memberUserId, startDate)
                endDate != null -> deleteGroupHistoryUntilDate(groupId, memberUserId, endDate)
            }
        } else {
            when {
                startDate != null && endDate != null -> deleteGroupHistoryByIdsAndBetweenDates(groupId, memberUserId, locationIds, startDate, endDate)
                startDate != null -> deleteGroupHistoryByIdsAndFromDate(groupId, memberUserId, locationIds, startDate)
                endDate != null -> deleteGroupHistoryByIdsAndUntilDate(groupId, memberUserId, locationIds, endDate)
            }
        }

        deleteUnusedGroupHistoryKeys(
            groupId = groupId,
            memberUserId = memberUserId,
            usedIds = getAllGroupHistoryKeysVersions(groupId = groupId, memberUserId = memberUserId)
        )

        deleteUnusedArchivedPersonalGroupKeys(
            groupId = groupId,
            usedIds = getAllUsedPersonalGroupKeysVersions(groupId = groupId)
        )
    }
}