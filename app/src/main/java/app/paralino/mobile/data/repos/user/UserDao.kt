package app.paralino.mobile.data.repos.user

import androidx.room.*
import app.paralino.mobile.data.models.user.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Transaction
    suspend fun save(user: UserEntity) {
        clear()
        create(user)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun create(user: UserEntity): Long

    @Query("SELECT * FROM user_me")
    fun get(): Flow<UserEntity?>

    @Query("SELECT * FROM user_me")
    suspend fun tryGetUser(): UserEntity?

    @Query("DELETE FROM user_me")
    suspend fun clear()

}