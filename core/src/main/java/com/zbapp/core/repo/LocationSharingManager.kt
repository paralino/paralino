package com.zbapp.core.repo

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.models.Resource
import com.zbapp.core.models.data
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.LocationSharingContract
import com.zbapp.core.models.toResource
import com.zbapp.core.repo.location.ISharingContractsLocalDataSource
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toTimeUnit

@OptIn(ExperimentalTime::class)
class LocationSharingManager(
    scopeProvider: ICoroutineScopeProvider,
    private val contractsDataSource: ISharingContractsLocalDataSource
) {

    companion object {
        val publicSharingMaxDurationMillis: Long = DurationUnit.HOURS.toTimeUnit().toMillis(5)
    }

    val allContractsFlow: StateFlow<Resource<List<LocationSharingContract>>> =
        contractsDataSource.getAllContracts()
            .map { it.mapNotNull { it.asValidContract() }.toResource() }
            .stateIn(
                scopeProvider.defaultScope,
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
            scopeProvider.defaultScope,
            SharingStarted.Lazily,
            Resource.NotInitialized
        )

    fun isSharingActive(): Boolean {
        return sharingStatus.value.data == true
    }

    fun isSharingActiveForGroup(id: String): Flow<Boolean> {
        return allContractsFlow.map {
            it.data?.any { it.groupId == id } ?: false
        }.distinctUntilChanged()
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

    private fun LocationSharingContract.asValidContract(): LocationSharingContract? {

        if(endTimeUTC == null) {
            // invalid public sharing contract
            // endless sharing for other groups
            if(this.groupId == UserGroup.publicSharingId) return null
            else return this
        } else {
            val date = LocalDateTime.parse(endTimeUTC).toInstant(TimeZone.UTC)
            val now = Clock.System.now()
            if(date <= now) {
                // expired
                return null
            } else {
                // limit public sharing if needed
                val maxPublic = now.plus(publicSharingMaxDurationMillis, DateTimeUnit.MILLISECOND)
                if(this.groupId == UserGroup.publicSharingId && date > maxPublic)
                    return this.copy(endTimeUTC = maxPublic.toLocalDateTime(TimeZone.UTC).toString())

                return this
            }
        }
    }

}

class NoValidContractsException: Exception("No valid location sharing contracts")