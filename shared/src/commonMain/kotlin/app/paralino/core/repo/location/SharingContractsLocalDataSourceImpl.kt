package app.paralino.core.repo.location

import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.utils.extensions.distinctFlowOfList
import app.paralino.core.models.mappers.SharingContractsMapper
import app.paralino.core.data.db.AppDatabaseHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SharingContractsLocalDataSourceImpl(
    private val database: AppDatabaseHelper,
    private val mapper: SharingContractsMapper,
) : ISharingContractsLocalDataSource {

    override suspend fun createContract(contract: LocationSharingContract) {
        database.sharingContractQueries.createContract(mapper.remote2Local(contract))
    }

    override fun getAllContracts(): Flow<List<LocationSharingContract>> {
        return database.mapToList { sharingContractQueries.getAllContracts() }
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    override suspend fun cancelContracts(ids: List<String>) {
        database.sharingContractQueries.deleteContract(ids)
    }

    override suspend fun cancelAll() {
        database.sharingContractQueries.clearAll()
    }
}