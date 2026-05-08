package app.paralino.core.db.main.common

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SharingContractDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createContract(contract: SharingContractEntity)

    @Query("SELECT * FROM sharingContractEntity WHERE groupId = :groupId")
    suspend fun getContractOnce(groupId: String): SharingContractEntity?

    @Query("SELECT * FROM sharingContractEntity WHERE groupId = :groupId")
    fun getContract(groupId: String): Flow<SharingContractEntity?>

    @Query("SELECT * FROM sharingContractEntity")
    fun getAllContracts(): Flow<List<SharingContractEntity>>

    @Query("SELECT * FROM sharingContractEntity")
    suspend fun getAllContractsOnce(): List<SharingContractEntity>

    @Transaction
    suspend fun cancelContract(groupId: String) {
        val currentContract = getContractOnce(groupId)

        if (currentContract != null) {
            createContract(
                contract = currentContract.copy(isEnabled = false)
            )
        } else {
            deleteContract(groupId)
        }
    }

    @Query("UPDATE OR IGNORE sharingContractEntity SET isEnabled = 0")
    suspend fun cancelAllContracts()

    @Query("DELETE FROM sharingContractEntity WHERE groupId = :groupId")
    suspend fun deleteContract(groupId: String)
}
