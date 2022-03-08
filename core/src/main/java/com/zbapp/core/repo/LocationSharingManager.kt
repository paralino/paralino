package com.zbapp.core.repo

import com.zbapp.core.ICoroutineScopeProvider
import com.zbapp.core.models.group.UserGroup
import com.zbapp.core.models.location.LocationSharingContract
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

    val allContractsFlow: StateFlow<List<LocationSharingContract>> =
        contractsDataSource.getAllContracts()
            .map { it.mapNotNull { it.asValidContract() } }
            .stateIn(
                scopeProvider.defaultScope,
                SharingStarted.Lazily,
                emptyList()
            )

    val sharingStatus: StateFlow<Boolean> = allContractsFlow.map { it.isNotEmpty() }
        .stateIn(
            scopeProvider.defaultScope,
            SharingStarted.Lazily,
            false
        )

    fun isSharingActive(): Boolean = sharingStatus.value

    fun isSharingActiveForGroup(id: String): Flow<Boolean> {
        return allContractsFlow.map {
            it.any { it.groupId == id }
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