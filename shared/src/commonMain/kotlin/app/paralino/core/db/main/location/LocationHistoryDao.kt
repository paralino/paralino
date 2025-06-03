package app.paralino.core.db.main.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import app.paralino.core.db.main.keys.GroupHistoryKeyEntity
import app.paralino.core.db.main.keys.PersonalHistoryKeyEntity

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
}