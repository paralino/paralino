package app.paralino.core.db.main.user

import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalCodeDao {

    @Transaction
    suspend fun recreateCodes(codes: List<PersonalCodeEntity>) {
        clearAll()
        createCodes(codes)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createCodes(code: List<PersonalCodeEntity>)

    @Query("SELECT * FROM personalCodeEntity")
    fun getAllCodes(): Flow<List<PersonalCodeEntity>>

    @Query("DELETE FROM personalCodeEntity WHERE id = :id")
    suspend fun deleteCode(id: String)

    @Query("DELETE FROM personalCodeEntity")
    suspend fun clearAll()
}
