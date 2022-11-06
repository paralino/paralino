package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.flow.*

class RealtimeEventsWhileSharingUseCase(
    private val realtimeApi: IRealtimeApi,
    private val placesRepo: PlacesRepository,
    private val groupRepo: GroupRepository,
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager
) {

    fun listenForEventsWhileSharing(): Flow<Unit> {
        val focusEvent =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionLocationFocusID}.documents"
        val placesEvent =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionPlacesID}.documents"
        val membersEvent =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionGroupMembersID}.documents"
        return realtimeApi.subscribeWithFilter(
            channels = listOf(focusEvent, placesEvent, membersEvent),
            filters = listOf(
                "$focusEvent.*.create",
                "$focusEvent.*.update",
                "$placesEvent.*.create",
                "$placesEvent.*.update",
                "$membersEvent.*",
            )
        )
            .onEach {
                when {
                    it.channels.contains(focusEvent) -> {
                        logg.tag("realtime_events_sharing").v("Got location focus create/update event")
                        val groupId = it.payload["groupId"] as String
                        if (locationSharingManager.isSharingActiveForGroup(groupId)) {
                            locationPerformanceManager.locationBoostRequested(groupId)
                        }
                    }
                    it.channels.contains(placesEvent) -> {
                        logg.tag("realtime_events_sharing").v("Got place create/update event")
                        val place = GroupPlaceRaw.from(it.payload)
                        placesRepo.updatePlaceLocally(place)
                    }
                    it.channels.contains(membersEvent) -> {
                        val member = if (it.events.any { it.endsWith(".delete") }) {
                            logg.tag("realtime_events_sharing").v("Got member delete event")
                            null
                        } else {
                            logg.tag("realtime_events_sharing").v("Got member create/update event")
                            MemberDocumentRaw.from(it.payload)
                        }

                        try {
                            if(member != null) {
                                groupRepo.gotMemberUpdateFromRealtime(member)
                            } else {
                                groupRepo.gotMemberDeleteFromRealtime(it.payload.get("groupId") as String)
                            }
                        } catch (e: Exception) {
                            logg.tag("realtime_events_sharing").e("Error with realtime member event", e)
                        }
                    }
                }
            }
            .catch { logg.tag("realtime_events_sharing").e("Error with realtime events while sharing", it) }
            .distinctUntilChanged()
            .map { }
    }

}