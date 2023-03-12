package app.paralino.core.domain.location

import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.location.LocationPerformanceManager
import kotlinx.coroutines.flow.*

class LocationRefreshUseCase constructor(
    private val realtimeApi: IRealtimeApi,
    private val realtimeEventsProcessor: RealtimeEventsProcessor,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val locationPerformanceManager: LocationPerformanceManager,
) {

    fun locationRefresh(groupId: String): Flow<Unit> {
        logg.tag("location_refresh").v("Start realtime subscription for group $groupId")
        val locationSubscriptionFlow = realtimeApi.subscribeWithFilter(
            channels = listOf(
                realtimeEventsProcessor.membersChannel,
                realtimeEventsProcessor.locationsChannel
            ),
            filters = listOf(
                "${realtimeEventsProcessor.membersChannel}.*",
                "${realtimeEventsProcessor.locationsChannel}.*"
            )
        )
            .onStart {
                logg.tag("location_refresh").v("Try refreshing group first")
                groupPreRefresh.refreshGroupWithMembers(groupId)
            }
            .onEach {
                realtimeEventsProcessor.processEvent(it)
            }
            .catch { logg.tag("realtime_events").e("Error parsing member or location on map", it) }

        val userMapFocusFlow = locationPerformanceManager.boostOnMapFlow(groupId)

        return combine(
            locationSubscriptionFlow,
            userMapFocusFlow
        ) { _, _ ->
            Unit
        }
    }

}