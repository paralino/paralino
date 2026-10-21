package app.paralino.core.repo.location

import app.paralino.core.db.MainDatabaseHelper
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.mappers.SharingContractsMapper
import app.paralino.core.utils.extensions.distinctFlowOfList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SharingContractsLocalDataSource(
    private val database: MainDatabaseHelper,
    private val mapper: SharingContractsMapper,
) {

    suspend fun createContract(contract: LocationSharingContract) {
        database.sharingContractsDao.createContract(mapper.remote2Local(contract))
    }

    suspend fun getContractOnce(groupId: String): LocationSharingContract? {
        return database.sharingContractsDao.getContractOnce(groupId = groupId)
            ?.let { mapper.local2Remote(it) }
    }

    fun getContract(groupId: String): Flow<LocationSharingContract?> {
        return database.sharingContractsDao.getContract(groupId = groupId)
            .distinctUntilChanged()
            .map { it?.let { model -> mapper.local2Remote(model) } }
    }

    // collected eagerly by LocationSharingManager, so it has to survive database reopening
    fun getAllContracts(): Flow<List<LocationSharingContract>> {
        return database.observe { sharingContractsDao.getAllContracts() }
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    suspend fun getAllContractsOnce(): List<LocationSharingContract> {
        return database.sharingContractsDao.getAllContractsOnce()
            .map { mapper.local2Remote(it) }
    }

    suspend fun cancelContract(id: String) {
        database.sharingContractsDao.cancelContract(id)
    }

    suspend fun cancelAllContracts() {
        database.sharingContractsDao.cancelAllContracts()
    }

    suspend fun deleteContract(id: String) {
        database.sharingContractsDao.deleteContract(id)
    }
}