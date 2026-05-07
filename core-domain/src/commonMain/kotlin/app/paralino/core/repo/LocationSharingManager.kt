package app.paralino.core.repo

import app.paralino.core.models.Resource
import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.toResource
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.repo.location.SharingContractsLocalDataSource
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LocationSharingManager(
    private val scopeProvider: ICoroutineScopeProvider,
    private val contractsDataSource: SharingContractsLocalDataSource
) {

    private val refreshTime = MutableStateFlow(nowUTC())

    val allContractsFlow: StateFlow<Resource<List<LocationSharingContract>>> =
        combine(
            refreshTime,
            contractsDataSource.getAllContracts()
                .map { it.mapNotNull { it.asValidContract() }.toResource() }
        ) { _, contracts ->
            contracts
        }
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
            it.data?.firstOrNull { it.groupOrSpaceId == id }?.asValidContract()
        }.distinctUntilChanged()
    }

    suspend fun getUnfilteredContractForGroupOnce(id: GroupOrSpaceId): LocationSharingContract? {
        return contractsDataSource.getContractOnce(groupId = id.id)
    }

    fun getUnfilteredContractForGroup(id: GroupOrSpaceId): Flow<LocationSharingContract?> {
        return contractsDataSource.getContract(groupId = id.id)
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
        return allContractsFlow.value.data?.any {
            it.asValidContract() != null
        } ?: false
    }

    suspend fun isSharingActive(): Boolean {
        return allValidContracts().isNotEmpty()
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

    suspend fun cancelAllContracts() {
        contractsDataSource.cancelAllContracts()
    }

    fun refreshAllContracts() {
        refreshTime.update { nowUTC() }
    }

    suspend fun allValidContracts(): List<LocationSharingContract> {
        return contractsDataSource.getAllContractsOnce().mapNotNull { it.asValidContract() }
    }
}

class NoValidContractsException: Exception("No valid location sharing contracts")