package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.location.LocationRepository
import kotlinx.coroutines.flow.*

class LocationRefreshUseCase(
    private val groupsRepo: GroupRepository,
    private val locationRepo: LocationRepository,
    private val locationPerformanceManager: LocationPerformanceManager,
) {

    fun locationRefresh(groupId: String): Flow<Unit> {

        val locationSubscriptionFlow = locationRepo.listenForMembersLocationUpdates(groupId)
            .onStart { groupsRepo.refreshGroupWithMembers(groupId) }
            .onEach { groupsRepo.gotMemberUpdateFromRealtime(it) }
            .catch { logg.e("Error parsing member on map", it) }

        val userMapFocusFlow = locationPerformanceManager.boostOnMapFlow(groupId)

        return combine(
            locationSubscriptionFlow,
            userMapFocusFlow
        ) { _, _ ->
            Unit
        }
    }

}