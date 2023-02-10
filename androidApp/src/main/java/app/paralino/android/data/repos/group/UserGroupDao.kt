package app.paralino.android.data.repos.group

import androidx.room.*
import app.paralino.android.data.models.group.UserGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGroupDao {

    @Transaction
    suspend fun replaceGroups(vararg data: UserGroupEntity) {
        clear()
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: UserGroupEntity)

    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<UserGroupEntity>>

    @Query("SELECT id FROM groups")
    fun getAllGroupIds(): Flow<List<String>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroup(id: String): Flow<UserGroupEntity?>

    @Query("SELECT ownerUserId FROM groups WHERE id = :groupId")
    suspend fun getGroupOwnerIdOnce(groupId: String): String?

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroup(id: String)

    @Query("DELETE FROM groups")
    suspend fun clear()

}