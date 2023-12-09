package app.paralino.core.domain.location

import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.group.CryptoKEKRepository
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.base.RealtimeResponse
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.location.LocationPerformanceManager
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.IPushProviderManager

class RealtimeEventsProcessor constructor(
    private val groupPreRefreshUseCase: GroupPreRefreshUseCase,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase,
    private val placesRepo: PlacesRepository,
    private val cryptoKEKRepo: CryptoKEKRepository,
    private val memberLocal: LocationLocalDataSource,
    private val locationSharingManager: LocationSharingManager,
    private val locationPerformanceManager: LocationPerformanceManager,
    private val pushProvider: IPushProviderManager
) {

    val locationFocusChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionLocationFocusID}.documents"
    val placesChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionPlacesID}.documents"
    val membersChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionGroupMembersID}.documents"
    val locationsChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionMemberLocationsID}.documents"
    val pushNotificationsChannel =
        "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionPushNotificationID}.documents"

    private fun isPaused(groupId: String): Boolean = registerDeviceForSharing.isProcessing(key = groupId)

    suspend fun processEvent(response: RealtimeResponse<Map<String, Any>>) {
        when {
            response.channels.contains(locationFocusChannel) -> {
                processLocationFocusChannelEvent(response)
            }

            response.channels.contains(placesChannel) -> {
                processPlaceChannelEvent(response)
            }

            response.channels.contains(membersChannel) -> {
                processMemberChannelEvent(response)
            }

            response.channels.contains(locationsChannel) -> {
                processMemberLocationChannelEvent(response)
            }

            response.channels.contains(pushNotificationsChannel) -> {
                processPushNotificationEvent(response)
            }
        }
    }

    private suspend fun processLocationFocusChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        logg.tag("realtime_events").v("Got location focus create/update event")
        val groupId = response.payload["groupId"] as String

        if (isPaused(groupId)) return

        if (locationSharingManager.isSharingActiveForGroup(groupId)) {
            locationPerformanceManager.locationBoostRequested(groupId)
        }
    }

    private suspend fun processPlaceChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        if (response.events.any { it.endsWith(".delete") }) {
            // do nothing for now
        } else {
            logg.tag("realtime_events").v("Got place create/update event")

            // don't update place if group actions are processing
            // place will be updated after everything is finished
            // key rotation is still in progress at this point so ignore this data
            val place = GroupPlaceRaw.from(response.payload)

            if (isPaused(place.groupId)) return

            if (!groupPreRefreshUseCase.isProcessing(place.groupId))
                placesRepo.updatePlaceLocally(place)
        }
    }

    private suspend fun processMemberChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        val member = if (response.events.any { it.endsWith(".delete") }) {
            logg.tag("realtime_events").v("Got member delete event")
            null
        } else {
            logg.tag("realtime_events").v("Got member create/update event")
            MemberDocumentRaw.from(response.payload)
        }

        try {
            if (member != null) {

                if (isPaused(member.groupId)) return

                if (groupPreRefreshUseCase.isProcessing(member.groupId)) {
                    logg.tag("realtime_events")
                        .v("Update in progress - Ignore member (${member.id}) update event in group ${member.groupId}")
                    return
                }

                val memberKey = cryptoKEKRepo.getMemberPublicKey(
                    groupId = member.groupId,
                    memberUserId = member.ownerUserId
                )

                if (memberKey !is GroupMemberMainPublicKey.Valid) {
                    logg.tag("realtime_events")
                        .v("Refresh group - update member (${member.id}) event in group ${member.groupId}")
                    // this is either new member so we are missing keys, or keys are obsolete
                    groupPreRefreshUseCase.refreshGroupWithMembers(
                        groupId = member.groupId,
                        cacheTimeMillis = 10_000
                    )
                } else {
                    logg.tag("realtime_events")
                        .v("Update member (${member.id}) event in group ${member.groupId}")
                    memberLocal.createOrUpdateMembersWithoutSettings(member)
                }
            } else {
                val groupId = response.payload["groupId"] as String

                if (isPaused(groupId)) return

                if (groupPreRefreshUseCase.isProcessing(groupId)) {
                    logg.tag("realtime_events")
                        .v("Update in progress - Ignore member delete event in group $groupId")
                    return
                }
                logg.tag("realtime_events").v("Delete member event in group $groupId")
                groupPreRefreshUseCase.refreshGroupWithMembers(groupId)
            }
        } catch (e: Exception) {
            logg.tag("realtime_events").e("Error with realtime member event", e)
        }
    }

    private suspend fun processMemberLocationChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        val location = if (response.events.any { it.endsWith(".delete") }) {
            logg.tag("realtime_events").v("Got location delete event")
            null
        } else {
            logg.tag("realtime_events").v("Got location create/update event")
            MemberLocationRaw.from(response.payload)
        }

        if (location != null) {
            val groupId = response.payload["groupId"] as String

            if (isPaused(groupId)) return

            if (groupPreRefreshUseCase.isProcessing(groupId)) {
                logg.tag("realtime_events")
                    .v("Update in progress - ignore location event in group $groupId")
                return
            }

            val hasDeviceKey =
                cryptoKEKRepo.hasDeviceKeyByVersion(deviceKeyVersion = location.deviceKeyVersion)
            val hasGroupKey =
                cryptoKEKRepo.hasGroupKeyByVersion(groupKeyVersion = location.groupKeyVersion)

            if (!hasDeviceKey || !hasGroupKey) {
                logg.tag("realtime_events")
                    .v("Location key version mismatch: groupKey ($hasGroupKey) - deviceKey ($hasDeviceKey)")
                val result = groupPreRefreshUseCase.refreshGroupWithMembers(
                    groupId = groupId,
                    cacheTimeMillis = 15_000
                )
                if (!result) {
                    memberLocal.createOrUpdateMemberLocation(location)
                }
            } else {
                memberLocal.createOrUpdateMemberLocation(location)
            }

        } else {
            val groupId = response.payload["groupId"] as String

            if (isPaused(groupId)) return

            if (groupPreRefreshUseCase.isProcessing(groupId)) {
                logg.tag("realtime_events")
                    .v("Update in progress - ignore location delete in group $groupId")
                return
            }
            logg.tag("realtime_events").v("Delete location event in group $groupId")
            groupPreRefreshUseCase.refreshGroupWithMembers(groupId)
        }
    }

    private fun processPushNotificationEvent(response: RealtimeResponse<Map<String, Any>>) {
        pushProvider.syncPushNotifications()
    }
}