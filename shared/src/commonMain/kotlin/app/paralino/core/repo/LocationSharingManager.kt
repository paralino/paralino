package app.paralino.core.repo

import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.models.Resource
import app.paralino.core.models.base.GroupOrSpaceId
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

    fun locationSharingStatus(onChange: (Resource<Boolean>) -> Unit) {
        sharingStatus.onEach { onChange(it) }.launchIn(scopeProvider.defaultIOScope)
    }

    fun sharingContractForGroup(id: GroupOrSpaceId): Flow<LocationSharingContract?> {
        return allContractsFlow.map {
            it.data?.firstOrNull { it.groupOrSpaceId == id }
        }.distinctUntilChanged()
    }

    suspend fun unfilteredContractForGroup(id: GroupOrSpaceId): LocationSharingContract? {
        return contractsDataSource.getContractOnce(groupId = id.id)
    }

    suspend fun currentSharingContractForGroup(id: GroupOrSpaceId): LocationSharingContract? {
        return contractsDataSource.getContractOnce(groupId = id.id)?.asValidContract()
    }

    fun isGroupSharingActiveMaybe(): Boolean {
        return allContractsFlow.value.data?.any {
            it.groupOrSpaceId.isGroup && it.asValidContract() != null
        } ?: false
    }

    fun isSharingActiveMaybe(): Boolean {
        return sharingStatus.value.data == true
    }

    suspend fun isSharingActive(): Boolean {
        return sharingStatus.value.data ?: allValidContracts().isNotEmpty()
    }

    fun isSharingActiveForGroupMaybe(id: GroupOrSpaceId): Boolean {
        return allContractsFlow.value.data?.firstOrNull { it.groupOrSpaceId == id }?.asValidContract() != null
    }

    suspend fun isSharingActiveForGroup(id: GroupOrSpaceId): Boolean {
        return currentSharingContractForGroup(id = id) != null
    }

    suspend fun startSharing(contract: LocationSharingContract) {
        contract.asValidContract()?.let {
            contractsDataSource.createContract(it)
        }
    }

    suspend fun stopSharing(groupId: GroupOrSpaceId, deleteContract: Boolean) {
        if (deleteContract) {
            contractsDataSource.deleteContract(id = groupId.id)
        } else {
            contractsDataSource.cancelContract(id = groupId.id)
        }
    }

    suspend fun allValidContracts(): List<LocationSharingContract> {
        return contractsDataSource.getAllContractsOnce().mapNotNull { it.asValidContract() }
    }
}

class NoValidContractsException: Exception("No valid location sharing contracts")