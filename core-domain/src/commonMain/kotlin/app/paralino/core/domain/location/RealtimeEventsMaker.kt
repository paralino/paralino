package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class RealtimeEventsMaker constructor(
    private val realtimeApi: IRealtimeApi,
    private val realtimeEventsProcessor: RealtimeEventsProcessor
) {
    val eventSetNotificationOnly = Pair(
        listOf(realtimeEventsProcessor.pushNotificationsChannel),
        listOf("${realtimeEventsProcessor.pushNotificationsChannel}.create")
    )

    val eventSetUiOnly = Pair(
        listOf(
            realtimeEventsProcessor.membersChannel,
            realtimeEventsProcessor.groupsLocationsChannel,
            realtimeEventsProcessor.personalLocationsChannel,
            realtimeEventsProcessor.personalDevicesChannel,
            realtimeEventsProcessor.placesChannel,
            realtimeEventsProcessor.privacyZonesChannel,
        ),
        listOf(
            realtimeEventsProcessor.membersChannel,
            realtimeEventsProcessor.groupsLocationsChannel,
            realtimeEventsProcessor.personalLocationsChannel,
            realtimeEventsProcessor.personalDevicesChannel,
            "${realtimeEventsProcessor.placesChannel}.upsert",
            "${realtimeEventsProcessor.privacyZonesChannel}.upsert",
        )
    )

    val eventSetEverything = Pair(
        eventSetUiOnly.first + eventSetNotificationOnly.first,
        eventSetUiOnly.second + eventSetNotificationOnly.second,
    )

    fun listenForEventsWhileActive(eventSet: Pair<List<String>, List<String>>): Flow<Unit> {
        logg.tag("realtime_events").v("Start realtime subscription")
        return realtimeApi.subscribeToChannels(
            channels = eventSet.second
        )
            .onEach {
                realtimeEventsProcessor.processEvent(it)
            }
            .catch {
                logg.tag("realtime_events").e("Error parsing realtime event data", it)
            }
            .distinctUntilChanged()
            .map { }
            .flowOn(Dispatchers.IO)
    }
}