package com.zbapp.location.repo.location

import com.zbapp.location.models.location.LocationSharingContract
import kotlinx.coroutines.flow.Flow

interface ISharingContractsLocalDataSource {
    suspend fun createContract(contract: LocationSharingContract)
    fun get(id: String): Flow<LocationSharingContract>
    fun getAllContracts(): Flow<List<LocationSharingContract>>
    suspend fun cancelContracts(ids: List<String>)
    suspend fun cancelAll()
}