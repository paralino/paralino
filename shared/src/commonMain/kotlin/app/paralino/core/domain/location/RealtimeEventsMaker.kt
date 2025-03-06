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
    fun listenForEventsWhileActive(): Flow<Unit> {
        logg.tag("realtime_events").v("Start realtime subscription for map")
        return realtimeApi.subscribeWithFilter(
            channels = listOf(
                realtimeEventsProcessor.membersChannel,
                realtimeEventsProcessor.locationsChannel,
                realtimeEventsProcessor.placesChannel,
                realtimeEventsProcessor.locationFocusChannel,
            ),
            filters = listOf(
                "${realtimeEventsProcessor.membersChannel}.*",
                "${realtimeEventsProcessor.locationsChannel}.*",
                "${realtimeEventsProcessor.placesChannel}.*.create",
                "${realtimeEventsProcessor.placesChannel}.*.update",
                "${realtimeEventsProcessor.locationFocusChannel}.*.create",
                "${realtimeEventsProcessor.locationFocusChannel}.*.update",
            )
        )
            .onEach {
                realtimeEventsProcessor.processEvent(it)
            }
            .catch {
                logg.tag("realtime_events")
                    .e("Error parsing member or location on map", it)
            }
            .distinctUntilChanged()
            .map { }
            .flowOn(Dispatchers.IO)
    }
}