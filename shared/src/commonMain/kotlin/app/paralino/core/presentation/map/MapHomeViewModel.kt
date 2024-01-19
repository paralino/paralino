package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.location.LocationRefreshUseCase
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberState
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.location.LocationRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.mapToPayload
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapHomeViewModel constructor(
    private val locationRepo: LocationRepository,
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val locationRefreshUseCase: LocationRefreshUseCase,
    private val userRepository: UserRepository,
    private val config: AppConfig,
    val valueFormatter: IValueFormatter
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
                                currentUserImage = it?.userImage ?: ImageModel.emoji(Emojis.robot)
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
                                            isCurrentUser = member.member.userId == config.userId,
                                            canHaveExpandButton = false
                                        ),
                                        actionItems = emptyList(),
                                        location = null,
                                        lastPing = null
                                    )
                                )
                            } else {
                                val memberState = GroupMemberState(
                                    member = member.member,
                                    isCurrentUser = member.member.userId == config.userId,
                                    canHaveExpandButton = false
                                )
                                member.locations.map { location ->

                                    val previousState = _uiState.value.asReady()?.members?.firstOrNull {
                                        it.memberState.memberId == memberState.memberId && it.location?.locationId == location.id
                                    }

                                    val canToggleOldLocation = if (location.isLocationAvailableButOld) {
                                        previousState?.location?.isShownAsOldLocation?.let { true } ?: true
                                    } else {
                                        false
                                    }

                                    val isLocationShown = if (canToggleOldLocation) {
                                        previousState?.location?.isShownAsOldLocation == true
                                    } else {
                                        location.hasLocation
                                    }

                                    val canDelete = member.member.userId == config.userId &&
                                            location.hasLocation &&
                                            !sharingManager.isSharingActiveForGroup(groupIdData.value)

                                    GroupMemberItemStateWithOneLocation(
                                        memberState = memberState,
                                        actionItems = buildList {
                                            if (canDelete)
                                                add(MemberAction.DELETE_LOCATION)

                                            if (location.canRequestLocation) {
                                                add(MemberAction.REQUEST_LOCATION)
                                            }
                                        },
                                        location = GroupMemberItemLocationState(
                                            memberLocation = location,
                                            name = location.name,
                                            image = member.member.image,
                                            canEdit = false,
                                            currentDevice = location.deviceId == config.deviceId,
                                            showSpeedAndElevation = true,
                                            formatter = valueFormatter,
                                            isShownAsOldLocation = if(canToggleOldLocation) isLocationShown else null
                                        ),
                                        lastPing = valueFormatter.getLastPing(location)
                                    )
                                }
                            }
                        }

                        val oldState = _uiState.value.asReady()?.takeIf { it.groupId == group.id }
                        val focusedMarker = oldState?.markers?.firstOrNull { it.focused }

                        val memberMarkers = group.members
                            .flatMap { member ->
                                member.locations.filter { it.locationData.data != null }
                                    .map { location ->
                                        val memberLocationState =
                                            membersData.first { it.location?.locationId == location.id }
                                        val oldCurrentMarker =
                                            focusedMarker?.takeIf { it.markerId == memberLocationState.keyId }
                                        val marker = MapPinData(
                                            location = location,
                                            name = listOfNotNull(
                                                member.member.name,
                                                location.nameOrNull
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?.let { "($it)" }
                                            ).joinToString(" "),
                                            image = member.member.image,
                                            timeInfo = valueFormatter.getLastPing(location)
                                        )
                                        marker.copy(
                                            markerId = memberLocationState.keyId,
                                            focused = oldCurrentMarker != null,
                                            hidden = oldCurrentMarker?.hidden ?: marker.hidden
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

                        val allMarkers = placeMarkers + memberMarkers
                        // only keep previous tracking marker if it still exists
                        val trackingMarker = oldState?.trackingMarker?.takeIf { oldMarker ->
                            allMarkers.any { it.markerId == oldMarker.markerId }
                        }

                        // only keep previous focus type if it still exists
                        val focusType = oldState?.mapHomeScreenFocusType?.takeIf { oldFocusType ->
                            when (oldFocusType) {
                                is MapHomeScreenFocusType.MEMBER -> {
                                    membersData.any { it.keyId == oldFocusType.memberKeyId }
                                }

                                is MapHomeScreenFocusType.PLACE -> {
                                    placesData.any { it.id == oldFocusType.placeId }
                                }

                                else -> true
                            }
                        }
                            ?: MapHomeScreenFocusType.ALL_MEMBERS.takeIf { memberMarkers.any { !it.hidden } }
                            ?: MapHomeScreenFocusType.ALL_PLACES.takeIf { placeMarkers.any { !it.hidden } }
                            ?: MapHomeScreenFocusType.NONE

                        MapHomeScreenState.Ready(
                            groupId = group.id,
                            groupName = group.name,
                            groupImage = group.image,
                            userCount = group.memberCount,
                            activeUserCount = group.members.filter { it.isActive }.size,
                            idleUserCount = group.members.filter { it.hasLocation && !it.isActive }.size,
                            currentUserImage = userData?.userImage
                                ?: ImageModel.emoji(Emojis.robot),
                            members = membersData,
                            places = placesData,
                            markers = placeMarkers + memberMarkers,
                            trackingMarker = trackingMarker,
                            mapHomeScreenFocusType = focusType,
                            sharingStatus = sharingStatus,
                            sharingEndTime = sharingStatus?.endTimeUTC?.let {
                                valueFormatter.fromUTCtoMapSharingTime(
                                    timeUTC = it,
                                    splitTimeToOneLine = true
                                )
                            },
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

    fun toggleMarkerVisibility(memberItem: GroupMemberItemState) {
        updateReadyState {
            val targetMarker = it.markers.firstOrNull { it.markerId == memberItem.keyId }
            it.copy(
                trackingMarker = if (targetMarker?.hidden != true) null else memberItem.asFocusedMarker(),
                markers = it.markers.map {
                    it.copy(
                        hidden = if (it.markerId == memberItem.keyId) false else it.hidden
                    )
                },
                mapHomeScreenFocusType = when (it.mapHomeScreenFocusType) {
                    is MapHomeScreenFocusType.MEMBER -> MapHomeScreenFocusType.MEMBER(
                        memberKeyId = memberItem.keyId,
                        forceZoom = false
                    )

                    else -> it.mapHomeScreenFocusType
                },
                members = it.members.map {
                    if (it.keyId == memberItem.keyId) {
                        it.copy(
                            location = it.location?.copy(
                                isShownAsOldLocation = it.location.isShownAsOldLocation?.let { true }
                            )
                        )
                    } else {
                        it
                    }
                }
            )
        }
    }

    fun trackMarker(markerData: FocusedMarkerData) {

        val marker = _uiState.value.asReady()
            ?.markers?.firstOrNull { it.markerId == markerData.markerId }

        val isRequestForSameVisibleMarker = _uiState.value.asReady()
            ?.trackingMarker?.markerId == markerData.markerId

        if (marker != null) {
            updateReadyState {
                it.copy(
                    trackingMarker = markerData.takeIf { !marker.hidden },
                    markers = it.markers.map {
                        it.copy(focused = it.markerId == markerData.markerId)
                    },
                    mapHomeScreenFocusType = when (markerData.type) {
                        MapPinType.MEMBER -> MapHomeScreenFocusType.MEMBER(
                            memberKeyId = markerData.markerId,
                            forceZoom = isRequestForSameVisibleMarker
                        )

                        MapPinType.PLACE -> MapHomeScreenFocusType.PLACE(placeId = markerData.markerId)
                    }
                )
            }
        } else {
            messageDispatcher.showSnackbar(MR.strings.group_member_no_location_info_message.localizedString())
        }
    }

    fun onMapTouch() = updateReadyState {
        if (it.mapHomeScreenFocusType.isMemberOrPlaceFocused) {
            // de-init focused marker so it is not tracked
            // and reset zoom enforcing for member focus
            it.copy(
                trackingMarker = null,
                mapHomeScreenFocusType = when (it.mapHomeScreenFocusType) {
                    is MapHomeScreenFocusType.MEMBER -> it.mapHomeScreenFocusType.copy(forceZoom = false)
                    else -> it.mapHomeScreenFocusType
                }
            )
        } else {
            it.copy(
                trackingMarker = null,
                markers = it.markers.map { it.copy(focused = false, hidden = it.obsolete) },
                mapHomeScreenFocusType = MapHomeScreenFocusType.NONE
            )
        }
    }

    fun hideFocusedItem() = updateReadyState {
        it.copy(
            trackingMarker = null,
            markers = it.markers.map { it.copy(focused = false, hidden = it.obsolete) },
            members = it.members.map {
                it.copy(location = it.location?.copy(isShownAsOldLocation = it.location.isShownAsOldLocation?.let { false }))
            },
            mapHomeScreenFocusType = MapHomeScreenFocusType.NONE
        )
    }

    fun toggleFocusOnMembers() = updateReadyState {
        it.copy(
            trackingMarker = null,
            markers = it.markers.map { it.copy(focused = false, hidden = it.obsolete) },
            mapHomeScreenFocusType = it.mapHomeScreenFocusType.toggle(MapHomeScreenFocusType.ALL_MEMBERS)
        )
    }

    fun toggleFocusOnPlaces() = updateReadyState {
        it.copy(
            trackingMarker = null,
            markers = it.markers.map { it.copy(focused = false, hidden = it.obsolete) },
            mapHomeScreenFocusType = it.mapHomeScreenFocusType.toggle(MapHomeScreenFocusType.ALL_PLACES)
        )
    }

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
                    clearMyLocation(state.location.locationId)
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
            .catch { messageDispatcher.showSnackbar(it.errorString()) }
            .launchIn(coroutineScope)
    }

    private fun requestLocationUpdate(state: GroupMemberItemStateWithOneLocation) {

        if (state.location == null) {
            coroutineScope.launch {
                messageDispatcher.showSnackbar(Throwable("Active device not found").errorString())
            }
            return
        }

        locationRefreshUseCase.refreshMemberDeviceLocation(
            groupId = state.memberState.groupId,
            memberUserId = state.memberState.userId,
            locationId = state.location.locationId
        )
            .catch { messageDispatcher.showSnackbar(it.errorString()) }
            .launchIn(coroutineScope)
    }
}