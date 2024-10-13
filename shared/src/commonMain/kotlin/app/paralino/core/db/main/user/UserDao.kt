package app.paralino.core.db.main.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Transaction
    suspend fun recreateUser(user: UserEntity) {
        clearUser()
        createUser(user)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUser(user: UserEntity)

    @Query("SELECT * FROM userEntity")
    fun getUser(): Flow<UserEntity?>

    @Query("DELETE FROM userEntity")
    suspend fun clearUser()

    @Transaction
    suspend fun clearUserData(
        subscriptionPlanDao: SubscriptionPlanDao,
        blocklistDao: BlocklistDao
    ) {
        clearUser()
        subscriptionPlanDao.clearAll()
        blocklistDao.clearAll()
    }
}