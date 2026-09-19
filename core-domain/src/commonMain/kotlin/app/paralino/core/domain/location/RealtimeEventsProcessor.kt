package app.paralino.core.domain.location

import app.paralino.core.crypto.GroupMemberMainPublicKey
import app.paralino.core.crypto.PersonalSpaceDeviceKey
import app.paralino.core.domain.crypto.group.CryptoKEKLocalRepository
import app.paralino.core.domain.crypto.user.CryptoUserKeysLocalSource
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.privacyzone.DeletePrivacyZoneUseCase
import app.paralino.core.domain.privacyzone.GetPrivacyZonesUseCase
import app.paralino.core.domain.user.PersonalSpaceRefreshUseCase
import app.paralino.core.domain.user.UserCryptoKeyRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.base.BackendDatabaseType
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.RealtimeResponse
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.models.raw.GroupPrivacyZoneRaw
import app.paralino.core.models.raw.MemberDocumentRaw
import app.paralino.core.models.raw.MemberLocationRaw
import app.paralino.core.models.raw.UserDeviceLocationRaw
import app.paralino.core.models.raw.UserDeviceRaw
import app.paralino.core.repo.location.LocationLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.IPushProviderManager
import app.paralino.core.repo.user.PersonalDevicesLocalDataSource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class RealtimeEventsProcessor constructor(
    private val groupPreRefreshUseCase: GroupPreRefreshUseCase,
    private val registerDeviceForSharing: RegisterDeviceForSharingUseCase,
    private val personalSpaceRefresh: PersonalSpaceRefreshUseCase,
    private val personalDevicesLocal: PersonalDevicesLocalDataSource,
    private val placesRepo: PlacesRepository,
    private val getPrivacyZonesUseCase: GetPrivacyZonesUseCase,
    private val deletePrivacyZoneUseCase: DeletePrivacyZoneUseCase,
    private val cryptoKEKRepo: CryptoKEKLocalRepository,
    private val userKeyRepo: CryptoUserKeysLocalSource,
    private val memberLocal: LocationLocalDataSource,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val refreshUserKey: UserCryptoKeyRefreshUseCase,
    private val pushProvider: IPushProviderManager
) {

    val placesChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionPlacesID}.rows"
    val privacyZonesChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionPrivacyZonesID}.rows"
    val membersChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionGroupMembersID}.rows"
    val groupsLocationsChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionMemberLocationsID}.rows"
    val personalLocationsChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionPersonalLocationsID}.rows"
    val personalDevicesChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionUserDevicesID}.rows"
    val pushNotificationsChannel =
        "tablesdb.${BackendDatabaseType.DATA.id}.tables.${BackendResourceIDs.collectionPushNotificationID}.rows"

    private fun isPaused(groupId: String): Boolean = registerDeviceForSharing.isProcessing(key = groupId)

    suspend fun processEvent(response: RealtimeResponse<Map<String, Any>>) {
        when {
            response.channels.contains(placesChannel) -> {
                processPlaceChannelEvent(response)
            }

            response.channels.contains(privacyZonesChannel) -> {
                processPrivacyZoneChannelEvent(response)
            }

            response.channels.contains(membersChannel) -> {
                processMemberChannelEvent(response)
            }

            response.channels.contains(groupsLocationsChannel) -> {
                processMemberLocationChannelEvent(response)
            }

            response.channels.contains(pushNotificationsChannel) -> {
                processPushNotificationEvent(response)
            }

            response.channels.contains(personalDevicesChannel) -> {
                processUserDeviceChannelEvent(response)
            }

            response.channels.contains(personalLocationsChannel) -> {
                processUserDeviceLocationChannelEvent(response)
            }
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

    private suspend fun processPrivacyZoneChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        val zone = GroupPrivacyZoneRaw.from(response.payload)

        if (isPaused(zone.groupId)) return
        if (groupPreRefreshUseCase.isProcessing(zone.groupId)) return

        if (response.events.any { it.endsWith(".delete") }) {
            logg.tag("realtime_events").v("Got privacy zone delete event")
            deletePrivacyZoneUseCase.deletePrivacyZoneLocally(
                groupId = zone.groupId,
                privacyZoneId = zone.id,
            )
        } else {
            logg.tag("realtime_events").v("Got privacy zone create/update event")
            getPrivacyZonesUseCase.updatePrivacyZoneLocally(zone)
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
                    // we are delaying since realtime fires before notification arrives, and not all data is yet formed on remote
                    delay(5.seconds)
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

            val hasDeviceKey = cryptoKEKRepo.hasDeviceKeyByVersion(
                locationId = location.id,
                deviceKeyVersion = location.deviceKeyVersion
            )
            val hasGroupKey = cryptoKEKRepo.hasGroupKeyByVersion(
                groupId = groupId,
                groupKeyVersion = location.groupKeyVersion
            )

            if (!hasDeviceKey || !hasGroupKey) {
                logg.tag("realtime_events")
                    .v("Location key version mismatch: groupKey ($hasGroupKey) - deviceKey ($hasDeviceKey)")
                val result = groupPreRefreshUseCase.refreshGroupWithMembers(
                    groupId = groupId,
                    cacheTimeMillis = 15_000
                )
                if (!result) {
                    saveLocationUseCase.saveGroupLocationLocally(location)
                }
            } else {
                saveLocationUseCase.saveGroupLocationLocally(location)
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

    private suspend fun processUserDeviceChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        val device = if (response.events.any { it.endsWith(".delete") }) {
            logg.tag("realtime_events").v("Got device delete event")
            null
        } else {
            logg.tag("realtime_events").v("Got device create/update event")
            UserDeviceRaw.from(response.payload)
        }

        if (device != null) {

            if (isPaused(UserGroup.groupPersonalDevicesId)) return

            if (device.userKeyVersion != null) {
                val hasUserKey = try {
                    userKeyRepo.personalKey(version = device.userKeyVersion)
                    true
                } catch (e: Exception) {
                    false
                }

                if (!hasUserKey) {
                    refreshUserKey.checkUserKeyStatus()
                }
            }

            personalDevicesLocal.updateUserDeviceData(device)
        } else {
            if (isPaused(UserGroup.groupPersonalDevicesId)) {
                return
            }

            personalSpaceRefresh.refreshPersonalSpace(cacheTimeMillis = 10.seconds.inWholeMilliseconds)
        }
    }

    private suspend fun processUserDeviceLocationChannelEvent(response: RealtimeResponse<Map<String, Any>>) {
        val location = if (response.events.any { it.endsWith(".delete") }) {
            logg.tag("realtime_events").v("Got location delete event")
            null
        } else {
            logg.tag("realtime_events").v("Got location create/update event")
            UserDeviceLocationRaw.from(response.payload)
        }

        if (location != null) {

            if (isPaused(UserGroup.groupPersonalDevicesId)) return

            val deviceKey = userKeyRepo.getDeviceSenderKeyForLocationId(locationId = location.id)
            if (deviceKey !is PersonalSpaceDeviceKey.Valid) {
                personalSpaceRefresh.refreshPersonalSpace(cacheTimeMillis = 10.seconds.inWholeMilliseconds)
            }

            saveLocationUseCase.savePersonalLocationLocally(location)
        } else {
            if (isPaused(UserGroup.groupPersonalDevicesId)) {
                return
            }

            personalSpaceRefresh.refreshPersonalSpace(cacheTimeMillis = 10.seconds.inWholeMilliseconds)
        }
    }

    private fun processPushNotificationEvent(response: RealtimeResponse<Map<String, Any>>) {
        logg.tag("realtime_events").v("Got notification document event")
        pushProvider.syncPushNotifications()
    }
}