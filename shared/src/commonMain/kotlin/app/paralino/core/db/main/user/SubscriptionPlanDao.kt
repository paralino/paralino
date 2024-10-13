package app.paralino.core.db.main.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlans(plans: List<SubscriptionPlanEntity>)

    @Transaction
    suspend fun recreatePlans(plans: List<SubscriptionPlanEntity>) {
        clearAll()
        createPlans(plans)
    }

    @Query("SELECT * FROM subscriptionPlanEntity")
    fun getAllPlans(): Flow<List<SubscriptionPlanEntity>>

    @Query("DELETE FROM subscriptionPlanEntity")
    suspend fun clearAll()
}