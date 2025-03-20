package app.paralino.core.db.main.common

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SharingContractDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createContract(contract: SharingContractEntity)

    @Query("SELECT * FROM sharingContractEntity WHERE groupId = :groupId")
    suspend fun getContractOnce(groupId: String): SharingContractEntity?

    @Query("SELECT * FROM sharingContractEntity")
    fun getAllContracts(): Flow<List<SharingContractEntity>>

    @Query("SELECT * FROM sharingContractEntity")
    suspend fun getAllContractsOnce(): List<SharingContractEntity>

    @Query("DELETE FROM sharingContractEntity WHERE groupId IN (:groupIds)")
    suspend fun deleteContract(groupIds: List<String>)

    @Query("DELETE FROM sharingContractEntity")
    suspend fun clearAll()
}
