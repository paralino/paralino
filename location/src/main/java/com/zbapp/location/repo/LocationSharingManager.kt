package com.zbapp.location.repo

import com.zbapp.location.models.circle.Circle
import com.zbapp.location.models.location.LocationSharingContract
import com.zbapp.location.utils.replace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
    val allContractsValue get() = userContracts.value.map { it.asValidContract() }.filterNotNull() +
            listOfNotNull(personalTracking.value.asValidContract(), publicSharing.value.asValidContract())

    val allContractsFlow get() = userContracts.map {
        it + listOfNotNull(personalTracking.value.asValidContract(), publicSharing.value.asValidContract())
    }


    fun startSharing(contract: LocationSharingContract) {
        val now = Clock.System.now().toEpochMilliseconds()
        val max = now + publicSharingMaxDurationMillis
        val end = contract.endTimeStamp

        when(contract.circleId) {
            publicSharing.value.circleId -> {
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
        replace { copy(endTimeStamp = null) }
    }

}