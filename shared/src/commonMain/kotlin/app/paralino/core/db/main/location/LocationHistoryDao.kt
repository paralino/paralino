package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPersonalKeys(entries: List<PersonalHistoryKeyEntity>)

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
        keys: List<GroupHistoryKeyEntity>
    ) {
        deleteOldGroupHistory(groupId = groupId, memberUserId = memberUserId, dates = dates)
        deleteUnusedHistoryKeys(
            groupId = groupId,
            memberUserId = memberUserId,
            usedIds = getAllHistoryKeysVersions(groupId = groupId, memberUserId = memberUserId)
        )
        createGroupHistory(entries = entries)
        createGroupKeys(keys = keys)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupHistory(entries: List<GroupHistoryEntity>)


    @Query("SELECT historyKeyVersion FROM groupHistoryEntity WHERE groupId = :groupId AND ownerUserId = :memberUserId")
    suspend fun getAllHistoryKeysVersions(groupId: String, memberUserId: String): List<String>

    @Query("DELETE FROM groupHistoryKeysEntity WHERE groupId = :groupId AND ownerUserId = :memberUserId AND historyKeyVersion NOT IN (:usedIds)")
    fun deleteUnusedHistoryKeys(groupId: String, memberUserId: String, usedIds: List<String>)



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

    @Query("SELECT * FROM personalHistoryKeysEntity WHERE historyKeyVersion IN (:keys)")
    suspend fun getPersonalHistoryKeysOnce(
        keys: List<String>
    ): List<PersonalHistoryKeyEntity>



    @Query("DELETE FROM personalHistoryEntity WHERE dateIdentifier IN (:dates)")
    suspend fun deleteOldPersonalHistory(dates: List<String>)

    @Query("DELETE FROM groupHistoryEntity WHERE groupId = :groupId AND ownerUserId = :memberUserId AND dateIdentifier IN (:dates)")
    suspend fun deleteOldGroupHistory(groupId: String, memberUserId: String, dates: List<String>)
}