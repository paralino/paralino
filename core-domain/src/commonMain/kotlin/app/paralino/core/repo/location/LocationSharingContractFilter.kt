package app.paralino.core.repo.location

import app.paralino.core.models.base.GroupOrSpaceId
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.utils.extensions.isInFuture
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class LocationSharingContractFilter {

    private val allowList = MutableStateFlow(mapOf<GroupOrSpaceId, Instant>())

    private fun getAllowedIds(): Set<GroupOrSpaceId> {
        return allowList.value.filter {
            it.value.isInFuture()
        }.keys
    }

    fun filter(contract: LocationSharingContract): LocationSharingContract? {
        return contract.takeIf { contract ->
            val allowedIds = getAllowedIds()
            if (allowedIds.isEmpty()) {
                true
            } else {
                allowedIds.any { it == contract.groupOrSpaceId }
            }
        }
    }

    fun setAsAllowedForSeconds(groupOrSpaceId: GroupOrSpaceId, seconds: Long) {
        setAsAllowedForDuration(groupOrSpaceId = groupOrSpaceId, duration = seconds.seconds)
    }

    fun setAsAllowedForDuration(groupOrSpaceId: GroupOrSpaceId, duration: Duration) {
        allowList.update { state ->
            state.filter { it.value.isInFuture() } + (groupOrSpaceId to nowUTC().plus(duration))
        }
    }

    fun clearFilter() {
        allowList.update { emptyMap() }
    }
}