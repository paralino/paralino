package com.zbapp.location.repo

import com.zbapp.location.models.circle.Circle
import com.zbapp.location.models.location.LocationSharingContract
import com.zbapp.location.repo.location.ISharingContractsLocalDataSource
import com.zbapp.location.utils.replace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import java.lang.Exception
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class LocationSharingManager(
    private val contractsDataSource: ISharingContractsLocalDataSource
) {

    companion object {
        @OptIn(ExperimentalTime::class)
        val publicSharingMaxDurationMillis: Long = DurationUnit.HOURS.toMillis(5)
    }

    val allContractsFlow: Flow<List<LocationSharingContract>> =
        contractsDataSource.getAllContracts()
            .map { it.mapNotNull { it.asValidContract() } }

    val sharingStatus: Flow<Boolean> = allContractsFlow.map { it.isNotEmpty() }.distinctUntilChanged()
    suspend fun isSharingActive(): Boolean = sharingStatus.firstOrNull() ?: false


    suspend fun startSharing(contract: LocationSharingContract) {
        contract.asValidContract()?.let {
            contractsDataSource.createContract(it)
        }
    }

    suspend fun stopAllSharing() {
        contractsDataSource.cancelAll()
    }

    suspend fun stopSharing(circleId: String) {
        contractsDataSource.cancelContracts(listOf(circleId))
    }

    suspend fun allValidContracts(): List<LocationSharingContract> {
        val all = contractsDataSource.getAllContracts().firstOrNull() ?: return emptyList()

        val valid = all.mapNotNull { it.asValidContract() }
        val expired = all.subtract(valid)

        expired.takeIf { it.isNotEmpty() }
            ?.map { it.circleId }
            ?.let { contractsDataSource.cancelContracts(it) }

        return valid
    }

    private fun LocationSharingContract.asValidContract(): LocationSharingContract? {

        if(endTimeStamp == null)
            return null

        // endless sharing
        if(circleId != Circle.publicSharing.id && endTimeStamp == 0L)
            return this

        // expired
        val now = Clock.System.now().toEpochMilliseconds()
        if(endTimeStamp <= now)
            return null

        // keep public sharing at max allowed duration
        if(circleId == Circle.publicSharing.id && endTimeStamp > now + publicSharingMaxDurationMillis)
            return copy(endTimeStamp = now + publicSharingMaxDurationMillis)

        return this
    }

}

class NoValidContractsException: Exception("No valid location sharing contracts")