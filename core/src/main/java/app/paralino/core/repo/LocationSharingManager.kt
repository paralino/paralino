package app.paralino.core.repo

import app.paralino.core.ICoroutineScopeProvider
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.toResource
import app.paralino.core.repo.location.ISharingContractsLocalDataSource
import kotlinx.coroutines.flow.*

class LocationSharingManager(
    scopeProvider: ICoroutineScopeProvider,
    private val contractsDataSource: ISharingContractsLocalDataSource
) {

    val allContractsFlow: StateFlow<Resource<List<LocationSharingContract>>> =
        contractsDataSource.getAllContracts()
            .map { it.mapNotNull { it.asValidContract() }.toResource() }
            .stateIn(
                scopeProvider.defaultIOScope,
                SharingStarted.Lazily,
                Resource.NotInitialized
            )

    val sharingStatus: StateFlow<Resource<Boolean>> = allContractsFlow.map {
        when(it) {
            Resource.NotInitialized -> Resource.NotInitialized
            is Resource.Ready -> Resource.Ready(!it.data.isNullOrEmpty())
        }
    }
        .stateIn(
            scopeProvider.defaultIOScope,
            SharingStarted.Lazily,
            Resource.NotInitialized
        )

    fun sharingStatusForGroup(id: String): Flow<Boolean> {
        return allContractsFlow.map {
            it.data?.any { it.groupId == id } ?: false
        }.distinctUntilChanged()
    }

    fun sharingContractForGroup(id: String): Flow<LocationSharingContract?> {
        return allContractsFlow.map {
            it.data?.firstOrNull { it.groupId == id }
        }.distinctUntilChanged()
    }

    fun isSharingActive(): Boolean {
        return sharingStatus.value.data == true
    }

    fun isSharingActiveForGroup(id: String): Boolean {
        return allContractsFlow.value.data?.any { it.groupId == id } ?: false
    }

    suspend fun startSharing(contract: LocationSharingContract) {
        contract.asValidContract()?.let {
            contractsDataSource.createContract(it)
        }
    }

    suspend fun stopAllSharing() {
        contractsDataSource.cancelAll()
    }

    suspend fun stopSharing(groupId: String) {
        contractsDataSource.cancelContracts(listOf(groupId))
    }

    suspend fun allValidContracts(): List<LocationSharingContract> {
        val all = contractsDataSource.getAllContracts().firstOrNull() ?: return emptyList()

        val valid = all.mapNotNull { it.asValidContract() }
        val expired = all.subtract(valid)

        expired.takeIf { it.isNotEmpty() }
            ?.map { it.groupId }
            ?.let { contractsDataSource.cancelContracts(it) }

        return valid
    }

}

class NoValidContractsException: Exception("No valid location sharing contracts")