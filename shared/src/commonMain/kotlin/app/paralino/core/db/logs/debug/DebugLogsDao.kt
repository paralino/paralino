package app.paralino.core.db.logs.debug

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DebugLogsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(log: DebugLogsEntity)

    @Transaction
    suspend fun insertNewAndRemoveOldEntries(
        maxValidTimeUTC: String,
        logs: List<DebugLogsEntity>
    ) {
        logs.forEach { insertEntry(it) }
        removeOlderThan(maxValidTimeUTC)
    }

    @Query(
        """
        SELECT * FROM debugLogsEntity
        WHERE
            (:typesNullIfEmpty IS 1 OR type IN (:types)) AND
            (:tagsNullIfEmpty IS 1 OR tag IN (:tags)) AND
            (:hostsNullIfEmpty IS 1 OR host IN (:hosts)) AND
            (:priorityNullIfEmpty IS 1 OR priority IN (:priorities)) AND
            (
                title LIKE '%' || :text || '%' OR
                info LIKE '%' || :text || '%' OR
                tag LIKE '%' || :text || '%'
            )
        ORDER BY id DESC
        """
    )
    fun getFiltered(
        typesNullIfEmpty: Int,
        tagsNullIfEmpty: Int,
        hostsNullIfEmpty: Int,
        priorityNullIfEmpty: Int,
        types: List<String>,
        tags: List<String>,
        hosts: List<String>,
        priorities: List<Int>,
        text: String
    ): Flow<List<DebugLogsEntity>>

    @Query("DELETE FROM debugLogsEntity WHERE id = :id")
    suspend fun remove(id: Int)

    @Query("DELETE FROM debugLogsEntity WHERE timeUTC < :maxValidTimeUTC")
    suspend fun removeOlderThan(maxValidTimeUTC: String)

    @Query("DELETE FROM debugLogsEntity")
    suspend fun clearAll()
}