package app.paralino.mobile.data.repos.personal_code

import androidx.room.*
import app.paralino.mobile.data.models.user.PersonalCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalCodeDao {

    @Transaction
    suspend fun replaceAll(vararg data: PersonalCodeEntity) {
        clear()
        createOrUpdate(*data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: PersonalCodeEntity)

    @Query("SELECT * FROM personal_codes")
    fun getAll(): Flow<List<PersonalCodeEntity>>

    @Query("DELETE FROM personal_codes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM personal_codes")
    suspend fun clear()

}