package app.paralino.core.repo.location

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.mappers.SharingContractsMapper
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SharingContractsLocalDataSource(
    private val database: MainDatabaseHelper,
    private val mapper: SharingContractsMapper,
) {

    suspend fun createContract(contract: LocationSharingContract) {
        database.sharingContractsDao.createContract(mapper.remote2Local(contract))
    }

    fun getAllContracts(): Flow<List<LocationSharingContract>> {
        return database.sharingContractsDao.getAllContracts()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    suspend fun cancelContracts(ids: List<String>) {
        database.sharingContractsDao.deleteContract(ids)
    }

    suspend fun cancelAll() {
        database.sharingContractsDao.clearAll()
    }
}