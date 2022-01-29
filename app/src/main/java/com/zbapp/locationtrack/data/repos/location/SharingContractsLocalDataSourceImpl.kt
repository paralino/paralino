package com.zbapp.locationtrack.data.repos.location

import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import com.zbapp.core.utils.distinctFlowOfList
import com.zbapp.locationtrack.data.mappers.SharingContractsMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SharingContractsLocalDataSourceImpl(
    private val dao: SharingContractsDao,
    private val mapper: SharingContractsMapper
) : ISharingContractsLocalDataSource {

    override suspend fun createContract(contract: LocationSharingContract) {
        dao.createOrUpdate(mapper.remote2Local(contract))
    }

    override fun get(id: String): Flow<LocationSharingContract> {
        return dao.get(id).map { mapper.local2Remote(it) }
    }

    override fun getAllContracts(): Flow<List<LocationSharingContract>> {
        return dao.getAll()
            .distinctFlowOfList()
            .map { it.map { mapper.local2Remote(it) } }
    }

    override suspend fun cancelContracts(ids: List<String>) {
        dao.clearForGroup(ids)
    }

    override suspend fun cancelAll() {
        dao.clear()
    }
}