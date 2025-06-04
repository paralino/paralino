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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlans(plans: List<SubscriptionPlanEntity>)

    @Transaction
    suspend fun recreatePlans(plans: List<SubscriptionPlanEntity>) {
        clearAllPlans()
        createPlans(plans)
    }

    @Query("SELECT * FROM subscriptionPlanEntity")
    fun getAllPlans(): Flow<List<SubscriptionPlanEntity>>

    @Query("DELETE FROM subscriptionPlanEntity")
    suspend fun clearAllPlans()

    @Query("DELETE FROM userEntity")
    suspend fun clearUser()

    @Transaction
    suspend fun clearUserAndPlans() {
        clearUser()
        clearAllPlans()
    }
}