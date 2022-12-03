package app.paralino.core.domain.location

import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.base.RealtimeResponse
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.ILocationLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.PlacesRepository

class RealtimeEventsProcessor constructor(
    private val groupPreRefreshUseCase: GroupPreRefreshUseCase,
    private val placesRepo: PlacesRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val memberLocal: ILocationLocalDataSource,
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager
) {

    val locationFocusChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionLocationFocusID}.documents"
    val placesChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionPlacesID}.documents"
    val membersChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionGroupMembersID}.documents"
    val locationsChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionMemberLocationsID}.documents"

    suspend fun processEvent(response: RealtimeResponse<Map<String, Any>>) {
        when {
            response.channels.contains(locationFocusChannel) -> {
                logg.tag("realtime_events_sharing").v("Got location focus create/update event")
                val groupId = response.payload["groupId"] as String
                if (locationSharingManager.isSharingActiveForGroup(groupId)) {
                    locationPerformanceManager.locationBoostRequested(groupId)
                }
            }
            response.channels.contains(placesChannel) -> {
                logg.tag("realtime_events_sharing").v("Got place create/update event")
                val place = GroupPlaceRaw.from(response.payload)
                gotPlaceUpdateFromRealtime(place)
            }
            response.channels.contains(membersChannel) -> {
                val member = if (response.events.any { it.endsWith(".delete") }) {
                    logg.tag("realtime_events").v("Got member delete event")
                    null
                } else {
                    logg.tag("realtime_events").v("Got member create/update event")
                    MemberDocumentRaw.from(response.payload)
                }

                try {
                    if(member != null) {
                        gotMemberUpdateFromRealtime(member)
                    } else {
                        gotMemberDeleteFromRealtime(response.payload.get("groupId") as String)
                    }
                } catch (e: Exception) {
                    logg.tag("realtime_events").e("Error with realtime member event", e)
                }
            }
        }
    }

    suspend fun gotPlaceUpdateFromRealtime(place: GroupPlaceRaw) {
        // don't update place if group actions are processing
        // place will be updated after everything is finished
        if(!groupPreRefreshUseCase.isProcessing(place.groupId))
            placesRepo.updatePlaceLocally(place)
    }

    suspend fun gotMemberDeleteFromRealtime(groupId: String) {
        logg.tag("realtime_events").v("Delete member event in group $groupId")
        groupPreRefreshUseCase.refreshGroupWithMembers(groupId)
    }

    suspend fun gotMemberUpdateFromRealtime(member: MemberDocumentRaw) {
        val memberKey = try {
            cryptoKEKRepo.getMemberKey(groupId = member.groupId, memberUserId = member.ownerUserId, memberPublicKey = member.ownerPublicKey)
        } catch (e: Exception) {
            null
        }

        if(memberKey == null || member.memberKeyVersion != memberKey.version) {
            logg.tag("realtime_events").v("Refresh group - update member (${member.id}) event in group ${member.groupId}")
            // this is either new member so we are missing keys, or keys are obsolete
            groupPreRefreshUseCase.refreshGroupWithMembers(member.groupId)
        } else {
            logg.tag("realtime_events").v("Update member (${member.id}) event in group ${member.groupId}")
            memberLocal.createOrUpdateMembers(member)
        }
    }

}