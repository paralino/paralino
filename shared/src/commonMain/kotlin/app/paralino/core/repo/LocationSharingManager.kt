package app.paralino.core.repo

import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.models.Resource
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.toResource
import app.paralino.core.repo.location.SharingContractsLocalDataSource
import kotlinx.coroutines.flow.*

class LocationSharingManager(
    private val scopeProvider: ICoroutineScopeProvider,
    private val contractsDataSource: SharingContractsLocalDataSource
) {

    val allContractsFlow: StateFlow<Resource<List<LocationSharingContract>>> =
        contractsDataSource.getAllContracts()
            .map { it.mapNotNull { it.asValidContract() }.toResource() }
            .stateIn(
                scopeProvider.defaultIOScope,
                SharingStarted.Eagerly,
                Resource.NotInitialized
            )

    val sharingStatus: StateFlow<Resource<Boolean>> = allContractsFlow.map {
        when(it) {
            Resource.NotInitialized -> Resource.NotInitialized
            is Resource.Ready -> Resource.Ready(it.data.any { it.asValidContract() != null })
        }
    }
        .stateIn(
            scopeProvider.defaultIOScope,
            SharingStarted.Eagerly,
            Resource.NotInitialized
        )

    fun sharingStatusForGroup(id: String): Flow<Boolean> {
        return allContractsFlow.map {
            it.data?.any { it.groupId == id } ?: false
        }.distinctUntilChanged()
    }

    fun locationSharingStatus(onChange: (Resource<Boolean>) -> Unit) {
        sharingStatus.onEach { onChange(it) }.launchIn(scopeProvider.defaultIOScope)
    }

    fun sharingContractForGroup(id: String): Flow<LocationSharingContract?> {
        return allContractsFlow.map {
            it.data?.firstOrNull { it.groupId == id }
        }.distinctUntilChanged()
    }

    fun currentSharingContractForGroup(id: String): LocationSharingContract? {
        return allContractsFlow.value.data?.firstOrNull { it.groupId == id }?.asValidContract()
    }

    fun isSharingActive(): Boolean {
        return sharingStatus.value.data == true
    }

    fun isSharingActiveForGroup(id: String): Boolean {
        return currentSharingContractForGroup(id = id) != null
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