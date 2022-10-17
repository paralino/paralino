package app.paralino.core.repo.location

import app.paralino.core.config
import app.paralino.core.logg
import app.paralino.core.models.BackendDatabaseType
import app.paralino.core.models.BackendResourceIDs
import app.paralino.core.models.group.UserGroupWithMembers
import app.paralino.core.models.location.LocationDataModel
import app.paralino.core.models.location.LocationPerformanceType
import app.paralino.core.models.location.MemberGroupId
import app.paralino.core.models.location.MemberModel
import app.paralino.core.models.raw.GroupPlaceRaw
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.NoValidContractsException
import app.paralino.core.repo.api.IRealtimeApi
import app.paralino.core.repo.base.BaseRepository
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import kotlinx.coroutines.flow.*

class LocationRepository constructor(
    val sharingManager: LocationSharingManager,
    private val local: ILocationLocalDataSource,
    private val remote: LocationRemoteSource,
    private val realtimeApi: IRealtimeApi,
    private val groupsRepos: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationPerformanceManager: LocationPerformanceManager,
) : BaseRepository() {

    fun locationRefresh(groupId: String): Flow<Unit> {

        val locationSubscriptionFlow = remote.listenForMembersLocationUpdates(groupId)
            .onStart { groupsRepos.refreshGroupWithMembers(groupId) }
            .onEach { local.createOrUpdateMembers(it) }
            .catch { logg.e("Error parsing member on map", it) }

        val userMapFocusFlow = locationPerformanceManager.boostOnMapFlow(groupId)

        return combine(
            locationSubscriptionFlow,
            userMapFocusFlow
        ) { _, _ ->
            Unit
        }
    }

    fun getLocationsForGroup(groupId: String): Flow<UserGroupWithMembers> {
        return groupsRepos.getGroupWithMembersLocally(groupId)
    }

    fun getMembersByUserId(userid: String): Flow<List<MemberModel>> {
        return local.getMemberModelsForUser(userid)
    }

    suspend fun pushLocation(data: LocationDataModel) {
        triggerRemoteOnly {
            if (!sharingManager.isSharingActive()) throw NoValidContractsException()

            val contracts = sharingManager.allValidContracts()
            val mapped = contracts.map { contract ->
                MemberGroupId(
                    memberId = local.getUserMemberIds(config.userId, contract.groupId)
                        // TODO use current device id for multiple device support
                        .first(),
                    groupId = contract.groupId,
                    // TODO share approximate location if selected
                    data = LocationDataModel(
                        timeUTC = data.timeUTC,
                        lat = data.lat,
                        lng = data.lng,
                        accuracy = data.accuracy,
                        batteryLevel = data.batteryLevel?.takeIf { contract.shareBattery },
                        speed = data.speed?.takeIf { contract.shareSpeed && it != 0f },
                        altitude = data.altitude?.takeIf { contract.shareElevation && it != 0f }
                    )
                )
            }

            placesRepo.processLocation(data)
            remote.saveCurrentLocation(mapped)
        }
    }

    fun listenForEventsWhileSharing(): Flow<Unit> {
        val focusEvent =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionLocationFocusID}.documents"
        val placesEvent =
            "databases.${BackendDatabaseType.DATA.id}.collections.${BackendResourceIDs.collectionPlacesID}.documents"
        return realtimeApi.subscribeWithFilter(
            channels = listOf(focusEvent, placesEvent),
            filters = listOf("$focusEvent.*.create", "$focusEvent.*.update", "$placesEvent.*.create", "$placesEvent.*.update")
        )
            .onEach {
                when {
                    it.channels.contains(focusEvent) -> {
                        logg.tag("realtime_events").v("Got member create/update event")
                        val groupId = it.payload["groupId"] as String
                        if (sharingManager.isSharingActiveForGroup(groupId)) {
                            locationPerformanceManager.locationBoostRequested(groupId)
                        }
                    }
                    it.channels.contains(placesEvent) -> {
                        logg.tag("realtime_events").v("Got place create/update event")
                        val place = GroupPlaceRaw.from(it.payload)
                        placesRepo.updatePlaceLocally(place)
                    }
                }
            }
            .distinctUntilChanged()
            .map { }
    }

    fun clearLocation(groupId: String): Flow<Unit> {
        return getDataOnce(
            localUpdate = {
                local.clearMemberLocationData(
                    local.getUserMemberIds(config.userId, groupId).first()
                )
            }
        ) {
            remote.saveCurrentLocation(
                ids = listOf(
                    MemberGroupId(
                        memberId = local.getUserMemberIds(config.userId, groupId).first(),
                        groupId = groupId,
                        data = null
                    )
                )
            )
        }
    }

    suspend fun clear() {
        sharingManager.stopAllSharing()
        local.clearAll()
    }

}

