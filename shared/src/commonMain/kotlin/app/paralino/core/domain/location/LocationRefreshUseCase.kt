package app.paralino.core.domain.location

import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationPerformanceManager
import kotlinx.coroutines.flow.*

class LocationRefreshUseCase(
    private val realtimeApi: IRealtimeApi,
    private val groupsRepo: GroupRepository,
    private val locationPerformanceManager: LocationPerformanceManager,
) {

    fun locationRefresh(groupId: String): Flow<Unit> {

        val event =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionGroupMembersID}.documents"

        val locationSubscriptionFlow = realtimeApi.subscribeWithFilter(
            channel = event,
            filters = listOf("$event.*.create", "$event.*.update")
        )
            .onStart { groupsRepo.refreshGroupWithMembers(groupId) }
            .onEach {
                val member = if (it.events.any { it.endsWith(".delete") }) {
                    logg.tag("realtime_events").v("Got member delete event")
                    null
                } else {
                    logg.tag("realtime_events").v("Got member create/update event")
                    MemberDocumentRaw.from(it.payload)
                }

                try {
                    if(member != null) {
                        groupsRepo.gotMemberUpdateFromRealtime(member)
                    } else {
                        groupsRepo.gotMemberDeleteFromRealtime(it.payload.get("groupId") as String)
                    }
                } catch (e: Exception) {
                    logg.tag("realtime_events").e("Error with realtime member event", e)
                }

            }
            .catch { logg.tag("realtime_events").e("Error parsing member on map", it) }

        val userMapFocusFlow = locationPerformanceManager.boostOnMapFlow(groupId)

        return combine(
            locationSubscriptionFlow,
            userMapFocusFlow
        ) { _, _ ->
            Unit
        }
    }

}