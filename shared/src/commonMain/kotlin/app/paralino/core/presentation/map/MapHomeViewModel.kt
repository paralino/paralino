package app.paralino.core.presentation.map

import app.paralino.core.domain.location.LocationRefreshUseCase
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.*
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.data.db.AppConfig
import app.paralino.core.logg
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapHomeViewModel constructor(
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationRefreshUseCase: LocationRefreshUseCase,
    private val userRepository: UserRepository,
    private val config: AppConfig,
    private val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MapHomeScreenState>(MapHomeScreenState.Empty())
    val uiState: Flow<MapHomeScreenState> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val groupIdData = MutableStateFlow("")

    private var locationSubscriptionJob: Job? = null

    val allGroups: StateFlow<Resource<List<UserGroup>>> = groupRepo.getAllGroupsLocally()
        .map { Resource.Ready(it) }
        .conflate()
        .distinctUntilChanged { old, new ->
            old.data.sortedBy { it.id }.toTypedArray()
                .contentDeepEquals(new.data.sortedBy { it.id }.toTypedArray())
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = Resource.NotInitialized
        )

    val allGroupsState: Flow<Boolean?> = allGroups.map {
        when (it) {
            Resource.NotInitialized -> null
            is Resource.Ready -> it.data.isNotEmpty()
        }
    }

    init {
        allGroups.onEach { groupsData ->
            if (groupsData is Resource.Ready) {
                val selectedGroup = groupIdData.value
                if (!groupsData.data.any { it.id == selectedGroup }) {
                    switchGroup(groupsData.data.firstOrNull()?.id ?: "")
                }
            }
        }.launchIn(coroutineScope)

        groupIdData
            .onEach { groupId ->
                keepLocationSubscriptionOn(groupId)
            }
            .flatMapLatest {
                if (it.isBlank()) {
                    userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                        .map {
                            MapHomeScreenState.Empty(
                                it?.userImage ?: ImageModel.emoji(Emojis.robot)
                            )
                        }
                } else {
                    combine(
                        groupRepo.getGroupWithMembersLocally(it),
                        placesRepo.getPlacesForGroupLocally(it),
                        getSharingContract(),
                        userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                    ) { group, places, sharingStatus, userData ->

                        val membersData = group.members.flatMap { member ->
                            if (member.locations.isEmpty()) {
                                listOf(
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = GroupMemberState(
                                            member = member.member,
                                            isCurrentUser = member.member.userId == config.userId
                                        ),
                                        location = null,
                                        lastPing = null
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
                                            actionItems = buildList {
                                                if (canDelete)
                                                    add(MemberAction.DELETE_LOCATION)

                                                if (location.canRequestLocation) {
                                                    add(MemberAction.REQUEST_LOCATION)
                                                }
                                            }
                                        ),
                                        location = GroupMemberItemLocationState(
                                            memberLocation = location,
                                            name = location.name,
                                            image = member.member.image,
                                            canEdit = false,
                                            currentDevice = location.deviceId == config.deviceId,
                                            showSpeedAndElevation = true,
                                            formatter = valueFormatter
                                        ),
                                        lastPing = valueFormatter.getLastPing(location)
                                    )
                                }
                            }
                        }

                        val focusedMarker = (_uiState.value as? MapHomeScreenState.Ready)?.focusedMarker

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

                        val placesData = places
                            .filter { it.data.data?.lat != null && it.data.data?.lng != null }
                            .map { PlaceListItemData(it) }

                        val placeMarkers = placesData.map { place ->
                            MapPinData(
                                groupId = group.id,
                                state = place
                            ).copy(
                                active = true,
                                focused = focusedMarker?.markerId == place.id
                            )
                        }

                        MapHomeScreenState.Ready(
                            groupId = group.id,
                            groupName = group.name,
                            groupImage = group.image,
                            userCount = group.memberCount,
                            activeUserCount = group.members.filter { it.isActive }.size,
                            idleUserCount = group.members.filter { it.hasLocation && !it.isActive }.size,
                            currentUserImage = userData?.userImage ?: ImageModel.emoji(Emojis.robot),
                            members = membersData,
                            places = placesData,
                            markers = placeMarkers + memberMarkers,
                            focusedMarker = focusedMarker,
                            mapHomeScreenFocusType = (_uiState.value as? MapHomeScreenState.Ready)?.mapHomeScreenFocusType ?: MapHomeScreenFocusType.ALL_MEMBERS,
                            sharingStatus = sharingStatus,
                            isSatelliteMode = _uiState.value.isSatelliteMode
                        )
                    }
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(coroutineScope)
    }

    fun switchGroup(groupId: String) {
        if (groupId.isBlank()) {
            val newGroupId =
                allGroups.value.data?.firstOrNull { it.id == config.selectedGroupId }?.id
                    ?: allGroups.value.data?.firstOrNull()?.id
                    ?: ""
            groupIdData.update { newGroupId }
            config.selectedGroupId = newGroupId
        } else {
            groupIdData.value = groupId
            config.selectedGroupId = groupId
        }
    }

    fun trackMarker(marker: FocusedMarkerData) {
        updateReadyState {
            it.copy(
                focusedMarker = marker,
                mapHomeScreenFocusType = when(marker.type) {
                    MapPinType.MEMBER -> MapHomeScreenFocusType.MEMBER
                    MapPinType.PLACE -> MapHomeScreenFocusType.PLACE
                }
            )
        }
    }

    fun hideFocusedItems() = updateReadyState { it.copy(focusedMarker = null, mapHomeScreenFocusType = MapHomeScreenFocusType.NONE) }
    fun toggleFocusOnMembers() = updateReadyState { it.copy(focusedMarker = null, mapHomeScreenFocusType = it.mapHomeScreenFocusType.toggle(MapHomeScreenFocusType.ALL_MEMBERS)) }
    fun toggleFocusOnPlaces() = updateReadyState { it.copy(focusedMarker = null, mapHomeScreenFocusType = it.mapHomeScreenFocusType.toggle(MapHomeScreenFocusType.ALL_PLACES)) }

    fun switchMapMode() {
        _uiState.update {
            when (it) {
                is MapHomeScreenState.Empty -> it.copy(isSatelliteMode = !it.isSatelliteMode)
                is MapHomeScreenState.Ready -> it.copy(isSatelliteMode = !it.isSatelliteMode)
            }
        }
    }

    private fun updateReadyState(action: (MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) {
        _uiState.update {
            when (it) {
                is MapHomeScreenState.Empty -> it
                is MapHomeScreenState.Ready -> action(it)
            }
        }
    }

    private fun getSharingContract(): Flow<LocationSharingContract?> {
        return sharingManager.allContractsFlow.combine(groupIdData) { contracts, group ->
            contracts.data?.firstOrNull { it.groupId == group }?.asValidContract()
        }
    }

    fun getAllGroups(): List<GroupItemState> {
        return allGroups.value.data?.map { group ->
            val uiType = when {
                !group.isVerified -> GroupItemState.GroupUiType.UNVERIFIED
                sharingManager.isSharingActiveForGroup(group.id) -> GroupItemState.GroupUiType.ACTIVE
                else -> GroupItemState.GroupUiType.INACTIVE
            }
            val isSelected = group.id == groupIdData.value
            GroupItemState(
                group,
                uiType,
                canEdit = true,
                isSelected = isSelected,
                isLarge = isSelected
            )
        } ?: emptyList()
    }

    private fun keepLocationSubscriptionOn(groupId: String?) {
        locationSubscriptionJob?.cancel()

        if (groupId.isNullOrBlank()) {
            logg.v("realtime", "Group id not valid, ignore realtime map subscription")
            return
        }

        logg.tag("location_performance").v("Map active for $groupId")
        locationSubscriptionJob = coroutineScope.launch {
            logg.v("realtime", "Group ready ($groupId), activate realtime subscription")
            locationRefreshUseCase.locationRefresh(groupId).mapToPayload { Unit }.collect()
        }
    }

    fun memberAction(action: MemberAction, state: GroupMemberItemState) {
        when (action) {
            MemberAction.DELETE_MEMBER,
            MemberAction.BLOCK_USER,
            MemberAction.UNBLOCK_USER -> {
            }

            MemberAction.DELETE_LOCATION -> {
                if (state is GroupMemberItemStateWithOneLocation && state.location != null) {
                    clearMyLocation(state.location!!.locationId)
                }
            }

            MemberAction.REQUEST_LOCATION -> {
                if (state is GroupMemberItemStateWithOneLocation) {
                    requestLocationUpdate(state)
                }
            }
        }
    }

    private fun clearMyLocation(locationId: String) {
        locationRepo.clearLocation(locationId)
            .catch { _eventChannel.send(MapHomeScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }

    private fun requestLocationUpdate(state: GroupMemberItemStateWithOneLocation) {

        if (state.location == null) {
            coroutineScope.launch {
                _eventChannel.send(MapHomeScreenEvent.Error(Throwable("Active device not found")))
            }
            return
        }

        locationRefreshUseCase.refreshMemberDeviceLocation(
            groupId = state.memberState.groupId,
            memberUserId = state.memberState.userId,
            locationId = state.location.locationId
        )
            .catch { _eventChannel.send(MapHomeScreenEvent.Error(it)) }
            .launchIn(coroutineScope)
    }
}