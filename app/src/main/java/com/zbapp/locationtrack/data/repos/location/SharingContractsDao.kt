package com.zbapp.locationtrack.data.repos.location

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zbapp.locationtrack.data.models.location.SharingContractEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharingContractsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(vararg data: SharingContractEntity)

    @Query("SELECT * FROM sharing_contracts")
    fun getAll(): Flow<List<SharingContractEntity>>

    @Query("SELECT * FROM sharing_contracts WHERE circleId = :circleId")
    fun get(circleId: String): Flow<SharingContractEntity>

    @Query("DELETE FROM sharing_contracts WHERE circleId IN (:ids)")
    suspend fun clearForCircle(ids: List<String>)

    @Query("DELETE FROM sharing_contracts")
    suspend fun clear()

}