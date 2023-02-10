package app.paralino.core.presentation.map

import app.paralino.core.domain.location.LocationRefreshUseCase
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.dashboard.GroupItemData
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberState
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.utils.IConfig
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapHomeViewModel constructor(
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationRefreshUseCase: LocationRefreshUseCase,
    private val config: IConfig
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<Payload<MapHomeScreenState>>(Payload.Loading)
    val uiState: Flow<Payload<MapHomeScreenState>> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val groupIdData = MutableStateFlow("")
    private val focusedMarker = MutableStateFlow<FocusedMarkerData?>(null)

    val locationRefreshSubscription = uiState.map { it.data?.groupId }
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { locationRefreshUseCase.locationRefresh(it).mapToPayload { Unit } }


    init {
        groupIdData
            .flatMapLatest {
                if (it.isBlank()) {
                    flowOf(Payload.Loading)
                } else {
                    combine(
                        // TODO if group is deleted, changes are not propagated here
                        groupRepo.getGroupWithMembersLocally(it),
                        placesRepo.getPlacesForGroupLocally(it),
                        getSharingContract(),
                        focusedMarker
                    ) { group, places, sharingStatus, focusedMarker ->

                        val membersData = group.members.flatMap { member ->

                            if (member.locations.isEmpty()) {
                                listOf(
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = GroupMemberState(
                                            member = member.member,
                                            isCurrentUser = member.member.userId == config.userId
                                        ),
                                        location = null
                                    )
                                )
                            } else {
                                val memberState = GroupMemberState(
                                    member = member.member,
                                    isCurrentUser = member.member.userId == config.userId
                                )
                                member.locations.map { location ->
                                    val canDelete = member.member.userId == config.userId &&
                                            location.locationData.data != null &&
                                            !sharingManager.isSharingActiveForGroup(groupIdData.value)
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = memberState.copy(
                                            actionItem = MemberAction.DELETE_LOCATION.takeIf { canDelete }
                                        ),
                                        location = GroupMemberItemLocationState(
                                            memberLocation = location,
                                            name = location.name.takeIf { member.locations.size > 1 },
                                            image = member.member.image,
                                            canEdit = false,
                                            currentDevice = location.deviceId == config.deviceId,
                                            showSpeedAndElevation = true
                                        )
                                    )
                                }
                            }
                        }

                        val memberMarkers = group.members
                            .flatMap { member ->
                                member.locations.filter { it.locationData.data != null }
                                    .map { location ->
                                        val memberLocationState =
                                            membersData.first { it.location?.locationId == location.id }
                                        MapPinData(
                                            location = location,
                                            image = member.member.image,
                                        ).copy(
                                            markerId = memberLocationState.keyId,
                                            focused = focusedMarker?.markerId == memberLocationState.keyId
                                        )
                                    }
                            }

                        val placeMarkers =
                            places.filter { it.data.data?.lat != null && it.data.data?.lng != null }
                                .map { place ->
                                    MapPinData(
                                        groupId = group.id,
                                        state = PlaceListItemData(place)
                                    ).copy(
                                        active = true,
                                        focused = focusedMarker?.markerId == place.id
                                    )
                                }

                        MapHomeScreenState(
                            groupId = group.id,
                            groupName = group.name,
                            groupImage = group.image,
                            userCount = group.memberCount,
                            inactiveUserCount = group.memberCount - group.members.filter { it.hasLocation }.size,
                            members = membersData,
                            markers = memberMarkers + placeMarkers,
                            focusedMarker = focusedMarker,
                            sharingStatus = sharingStatus
                        )
                    }
                        .mapToPayload { it }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(coroutineScope)
//        switchGroup("")
    }

    fun switchGroup(groupId: String) {
        if (groupId.isBlank()) {
            _uiState.value = Payload.Loading
            coroutineScope.launch {
                val groupIds = groupRepo.getAllGroupIds().firstOrNull() ?: emptyList()
                groupIdData.value = groupIds.firstOrNull { it == config.selectedGroupId }
                        ?: groupIds.firstOrNull()
                        ?: ""
            }
        } else {
            groupIdData.value = groupId
            config.selectedGroupId = groupId
        }
    }

    fun trackMarker(marker: FocusedMarkerData?) {
        focusedMarker.update { marker }
    }

    private fun getSharingContract(): Flow<LocationSharingContract?> {
        return locationRepo.sharingManager.allContractsFlow.combine(groupIdData) { contracts, group ->
            contracts.data?.firstOrNull { it.groupId == group }?.asValidContract()
        }
    }

    suspend fun getAllGroups(): List<GroupItemData> {
        return groupRepo.getAllGroupsLocally().firstOrNull()?.map { GroupItemData(it) }
            ?: emptyList()
    }

    fun clearMyLocation(locationId: String) {
        locationRepo.clearLocation(locationId)
            .catch { _eventChannel.send(MapHomeScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }

}