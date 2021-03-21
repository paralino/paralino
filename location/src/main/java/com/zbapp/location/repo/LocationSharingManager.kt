package com.zbapp.location.repo

import com.zbapp.location.models.circle.Circle
import com.zbapp.location.models.location.LocationSharingContract
import com.zbapp.location.utils.replace
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class LocationSharingManager {

    companion object {
        @OptIn(ExperimentalTime::class)
        val publicSharingMaxDurationMillis: Long = DurationUnit.HOURS.toMillis(5)
    }

    private val personalTracking = MutableStateFlow(LocationSharingContract(Circle.personalTracking.id, null))
    private val publicSharing = MutableStateFlow(LocationSharingContract(Circle.publicSharing.id, null))

    private val userContracts = MutableStateFlow(emptyList<LocationSharingContract>())

    val allContractsValue: List<LocationSharingContract>
        get() = userContracts.value.toMutableList().apply {
            add(personalTracking.value)
            add(publicSharing.value)
        }.mapNotNull { it.asValidContract() }

    val allContractsFlow: Flow<List<LocationSharingContract>> =
        combine(userContracts, personalTracking, publicSharing) { u, m, p ->
            u.toMutableList().apply {
                add(m)
                add(p)
            }.mapNotNull { it.asValidContract() }
        }

    val sharingStatus: Flow<Boolean> = allContractsFlow.map { it.isNotEmpty() }.distinctUntilChanged()
    val isSharingActive: Boolean get() = allContractsValue.isNotEmpty()

    fun startSharing(contract: LocationSharingContract) {
        val now = Clock.System.now().toEpochMilliseconds()
        val end = contract.endTimeStamp

        when(contract.circleId) {
            publicSharing.value.circleId -> {
                val max = now + publicSharingMaxDurationMillis
                publicSharing.newValue(
                    when {
                        end == null || end < now -> null
                        end > max -> max
                        else -> end
                    }
                )
            }
            personalTracking.value.circleId -> {
                personalTracking.newValue(
                    when {
                        end == 0L -> 0L
                        end == null || end < now -> null
                        else -> end
                    }
                )
            }
            else -> {
                val contract = contract.copy(
                    endTimeStamp = when {
                        end == 0L -> 0L
                        end == null || end < now -> null
                        else -> end
                    }
                )

                userContracts.replace {
                    toMutableList().apply {
                        removeAll { it.circleId == contract.circleId }
                        contract.asValidContract()?.let {
                            add(it)
                        }
                    }
                }
            }
        }
    }

    fun stopAllSharing() {
        publicSharing.newValue(null)
        personalTracking.newValue(null)
        userContracts.value = emptyList()
    }

    fun stopSharing(circleId: String) {
        when(circleId) {
            publicSharing.value.circleId -> publicSharing.newValue(null)
            personalTracking.value.circleId -> personalTracking.newValue(null)
            else -> {
                userContracts.replace {
                    toMutableList().apply {
                        removeAll { it.circleId == circleId }
                    }
                }
            }
        }
    }

    private fun MutableStateFlow<LocationSharingContract>.newValue(time: Long?) {
        replace { copy(endTimeStamp = time) }
    }

}