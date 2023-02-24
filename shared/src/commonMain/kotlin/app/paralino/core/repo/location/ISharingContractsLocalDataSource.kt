package app.paralino.core.repo.location

import app.paralino.core.models.location.LocationSharingContract
import kotlinx.coroutines.flow.Flow

interface ISharingContractsLocalDataSource {
    suspend fun createContract(contract: LocationSharingContract)
    fun getAllContracts(): Flow<List<LocationSharingContract>>
    suspend fun cancelContracts(ids: List<String>)
    suspend fun cancelAll()
}