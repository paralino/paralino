package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import com.zbapp.locationtrack.data.mappers.SharingContractsMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SharingContractsLocalDataSourceImpl(
    private val dao: SharingContractsDao
): ISharingContractsLocalDataSource {

    override suspend fun createContract(contract: LocationSharingContract) {
        dao.createOrUpdate(SharingContractsMapper.mapReversed(contract))
    }

    override fun get(id: String): Flow<LocationSharingContract> {
        return dao.get(id).map { SharingContractsMapper.map(it) }
    }

    override fun getAllContracts(): Flow<List<LocationSharingContract>> {
        return dao.getAll().map { it.map { SharingContractsMapper.map(it) } }
    }

    override suspend fun cancelContracts(ids: List<String>) {
        dao.clearForCircle(ids)
    }

    override suspend fun cancelAll() {
        dao.clear()
    }
}