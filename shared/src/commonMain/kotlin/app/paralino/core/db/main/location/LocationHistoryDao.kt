package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    suspend fun createGroupKeys(entries: List<GroupHistoryKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createGroupHistory(entries: List<GroupHistoryEntity>)



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

    @Query("SELECT * FROM groupHistoryKeyEntity WHERE groupId = :groupId AND ownerUserId = :ownerUserId AND historyKeyVersion IN (:keys)")
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
    )

    @Query("SELECT * FROM personalHistoryKeyEntity WHERE historyKeyVersion IN (:keys)")
    suspend fun getPersonalHistoryKeysOnce(
        keys: List<String>
    ): List<PersonalHistoryKeyEntity>



    @Query("DELETE FROM personalHistoryEntity WHERE dateIdentifier IN (:dates)")
    suspend fun deleteOldPersonalHistory(dates: List<String>)
}