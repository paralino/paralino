package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class RealtimeEventsMaker constructor(
    private val realtimeApi: IRealtimeApi,
    private val realtimeEventsProcessor: RealtimeEventsProcessor
) {

    // TODO remove location focus subscription
    fun listenForEventsWhileSharing(): Flow<Unit> {
        return realtimeApi.subscribeWithFilter(
            channels = listOf(
                realtimeEventsProcessor.locationFocusChannel,
                realtimeEventsProcessor.placesChannel,
                realtimeEventsProcessor.membersChannel,
                realtimeEventsProcessor.locationsChannel,
                realtimeEventsProcessor.pushNotificationsChannel,
            ),
            filters = listOf(
                "${realtimeEventsProcessor.locationFocusChannel}.*.create",
                "${realtimeEventsProcessor.locationFocusChannel}.*.update",
                "${realtimeEventsProcessor.placesChannel}.*.create",
                "${realtimeEventsProcessor.placesChannel}.*.update",
                "${realtimeEventsProcessor.membersChannel}.*",
                "${realtimeEventsProcessor.locationsChannel}.*",
                "${realtimeEventsProcessor.pushNotificationsChannel}.*.create",
            )
        )
            .onEach {
                realtimeEventsProcessor.processEvent(it)
            }
            .catch {
                logg.tag("realtime_events_sharing")
                    .e("Error with realtime events while sharing", it)
            }
            .distinctUntilChanged()
            .map { }
    }

    fun listenForEventsWhileActive(): Flow<Unit> {
        logg.tag("realtime_events").v("Start realtime subscription for map")
        return realtimeApi.subscribeWithFilter(
            channels = listOf(
                realtimeEventsProcessor.membersChannel,
                realtimeEventsProcessor.locationsChannel,
                realtimeEventsProcessor.pushNotificationsChannel
            ),
            filters = listOf(
                "${realtimeEventsProcessor.membersChannel}.*",
                "${realtimeEventsProcessor.locationsChannel}.*",
                "${realtimeEventsProcessor.pushNotificationsChannel}.*.create"
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
    }
}