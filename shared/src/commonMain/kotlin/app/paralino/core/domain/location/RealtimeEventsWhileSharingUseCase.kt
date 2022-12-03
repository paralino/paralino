package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.repo.api.IRealtimeApi
import kotlinx.coroutines.flow.*

class RealtimeEventsWhileSharingUseCase constructor(
    private val realtimeApi: IRealtimeApi,
    private val realtimeEventsProcessor: RealtimeEventsProcessor
) {

    fun listenForEventsWhileSharing(): Flow<Unit> {
        return realtimeApi.subscribeWithFilter(
            channels = listOf(
                realtimeEventsProcessor.locationFocusChannel,
                realtimeEventsProcessor.placesChannel,
                realtimeEventsProcessor.membersChannel,
                realtimeEventsProcessor.locationsChannel
            ),
            filters = listOf(
                "${realtimeEventsProcessor.locationFocusChannel}.*.create",
                "${realtimeEventsProcessor.locationFocusChannel}.*.update",
                "${realtimeEventsProcessor.placesChannel}.*.create",
                "${realtimeEventsProcessor.placesChannel}.*.update",
                "${realtimeEventsProcessor.membersChannel}.*",
                "${realtimeEventsProcessor.locationsChannel}.*",
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

}