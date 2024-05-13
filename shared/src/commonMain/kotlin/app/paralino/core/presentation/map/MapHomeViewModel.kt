package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.data.db.AppConfig
import app.paralino.core.domain.group.GroupPreRefreshUseCase
import app.paralino.core.domain.location.ClearUserLocationUseCase
import app.paralino.core.domain.location.LocationRequestUseCase
import app.paralino.core.domain.location.RealtimeEventsMaker
import app.paralino.core.logg
import app.paralino.core.models.Resource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroupWithMetadata
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.CoreViewModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberState
import app.paralino.core.presentation.groups.LocationRequestItemState
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberActionsState
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.IValueFormatter
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.LocationSharingManager
import app.paralino.core.repo.group.GroupRepository
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.mapToPayload
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MapHomeViewModel constructor(
    private val sharingManager: LocationSharingManager,
    private val groupRepo: GroupRepository,
    private val placesRepo: PlacesRepository,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val locationRequestUseCase: LocationRequestUseCase,
    private val clearUserLocation: ClearUserLocationUseCase,
    private val realtimeEvents: RealtimeEventsMaker,
    private val userRepository: UserRepository,
    private val config: AppConfig,
    private val deviceApiProvider: IDeviceApiProvider,
    val valueFormatter: IValueFormatter
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MapHomeScreenState>(MapHomeScreenState.Empty())
    val uiState: Flow<MapHomeScreenState> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val groupIdData = MutableStateFlow("")

    private var activePingSubscriptionJob: Job? = null
    private var realtimeSubscriptionJob: Job? = null
    private val locationRequestDuration = 20.seconds
    private val locationRequestDelay = 2.minutes

    private val timeRefreshPingFlow = flow<Unit> {
        emit(Unit)
        delay(20)
        while (currentCoroutineContext().isActive) {
            delay(5.seconds)
            emit(Unit)
        }
    }

    val allGroups: StateFlow<Resource<List<UserGroupWithMetadata>>> =
        groupRepo.getAllGroupsLocally()
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

    /**
     * Used in iOS
     */
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

        combine(
            groupIdData,
            deviceApiProvider.appLifecycleState
                .map { it.current.activeOrInactive }
                .distinctUntilChanged(),
            uiState.map { it.asReady()?.mapHomeScreenFocusType }
                .filterNotNull()
                .distinctUntilChanged()
        ) { _, _, focusType ->
            focusType
        }
            .debounce(2000)
            .distinctUntilChanged()
            .onEach {
                initRealtime()
                keepLocationFocusOn(focusType = it)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        groupIdData
            .onEach {
                if (it.isNotBlank()) {
                    coroutineScope.launch {
                        groupPreRefresh.refreshGroupWithMembers(it)
                    }
                }
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
                        timeRefreshPingFlow,
                        groupRepo.getGroupWithMembersLocally(it),
                        placesRepo.getPlacesForGroupLocally(it),
                        getSharingContract(),
                        userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                    ) { _, group, places, sharingStatus, userData ->

                        if (group == null) {
                            return@combine MapHomeScreenState.Empty()
                        }

                        val membersData = group.members.flatMap { member ->
                            if (member.locations.isEmpty()) {
                                listOf(
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = GroupMemberState(
                                            member = member.member,
                                            isCurrentUser = member.member.userId == config.userId,
                                            canHaveExpandButton = false
                                        ),
                                        actions = MemberActionsState(),
                                        location = null
                                    )
                                )
                            } else {
                                val memberState = GroupMemberState(
                                    member = member.member,
                                    isCurrentUser = member.member.userId == config.userId,
                                    canHaveExpandButton = false
                                )
                                member.locations.map { location ->

                                    val previousState =
                                        _uiState.value.asReady()?.members?.firstOrNull {
                                            it.memberState.memberId == memberState.memberId && it.location?.locationId == location.id
                                        }

                                    val locationVisibility =
                                        if (location.isLocationAvailableButOld) {
                                            previousState?.location?.visibility
                                                ?: MemberLocationVisibilityType.HIDDEN
                                        } else if (location.hasLocation) {
                                            MemberLocationVisibilityType.VISIBLE_ALWAYS
                                        } else {
                                            MemberLocationVisibilityType.MISSING
                                        }

                                    GroupMemberItemStateWithOneLocation(
                                        memberState = memberState,
                                        actions = MemberActionsState(
                                            deleteLocation = takeIf {
                                                canDeleteLocation(
                                                    groupId = group.id,
                                                    isCurrentUser = member.member.userId == config.userId,
                                                    isCurrentDevice = location.deviceId == config.deviceId,
                                                    hasLocation = locationVisibility.isVisible()
                                                )
                                            }?.let {
                                                MemberActionItemState(
                                                    type = MemberActionType.DELETE_LOCATION,
                                                    isLoading = previousState?.actions?.deleteLocation?.isLoading
                                                        ?: false,
                                                )
                                            },
                                            locationRequest = takeIf { location.canRequestLocation }?.let {
                                                val isRequestInProgress =
                                                    previousState?.actions?.locationRequest?.run { isLoading && !isWaitingForUser }
                                                        ?: false
                                                val hasUserResponded =
                                                    (previousState?.actions?.locationRequest?.lastLocationRequest?.toEpochMilliseconds()
                                                        ?: 0) < (location.status.data?.timeUTC?.fromUTCtoInstant()
                                                        ?.plus(2.seconds)?.toEpochMilliseconds()
                                                        ?: 0)
                                                val isRequestExpired =
                                                    previousState?.actions?.locationRequest?.lastLocationRequest
                                                        ?.plus(locationRequestDuration)
                                                        ?.let { it < nowUTC() }
                                                        ?: true
                                                val canRequestLocationAgain =
                                                    previousState?.actions?.locationRequest?.lastLocationRequest
                                                        ?.plus(locationRequestDelay)
                                                        ?.let { it < nowUTC() }
                                                        ?: true

                                                if (hasUserResponded && !isRequestInProgress) {
                                                    LocationRequestItemState(
                                                        isEnabled = canRequestLocationAgain,
                                                        lastLocationRequest = previousState?.actions?.locationRequest?.lastLocationRequest
                                                    )
                                                } else if (isRequestExpired && !isRequestInProgress) {
                                                    LocationRequestItemState(
                                                        isEnabled = canRequestLocationAgain,
                                                        lastLocationRequest = previousState?.actions?.locationRequest?.lastLocationRequest
                                                    )
                                                } else {
                                                    previousState?.actions?.locationRequest
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
                                            visibility = locationVisibility,
                                            formatter = valueFormatter
                                        )
                                    )
                                }
                            }
                        }
                            .sortedByDescending { it.location?.deviceStatus == MemberDeviceStatusType.ACTIVE }
                            .sortedByDescending { it.location?.deviceStatus == MemberDeviceStatusType.ACTIVE_NO_LOCATION }
                            .sortedByDescending { it.location?.deviceStatus?.isIdlingOrActive }
                            .sortedByDescending { it.location?.visibility?.isVisible() }


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
                                            timeInfo = valueFormatter.getLastLocationPing(location)
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

                        val mapSheetSelectedContentType = oldState?.mapSheetSelectedContentType
                            ?: MapSheetSelectedContentType.MEMBERS

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
                            mapSheetSelectedContentType = mapSheetSelectedContentType,
                            sharingStatus = sharingStatus,
                            sharingEndTime = sharingStatus?.endTimeUTC?.let {
                                valueFormatter.fromUTCtoMapSharingTime(
                                    timeUTC = it,
                                    splitTimeToOneLine = true
                                )
                            },
                            verified = group.isGroupVerified,
                            isSatelliteMode = _uiState.value.isSatelliteMode
                        )
                    }
                        .flowOn(Dispatchers.IO)
                }
            }
            .onEach { _uiState.value = it }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun canDeleteLocation(
        groupId: String,
        isCurrentUser: Boolean,
        isCurrentDevice: Boolean,
        hasLocation: Boolean
    ): Boolean {
        return isCurrentUser
                && hasLocation
                && (!isCurrentDevice || !sharingManager.isSharingActiveForGroup(groupId))
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
                    it.copy(hidden = if (it.markerId == memberItem.keyId) false else it.hidden)
                },
                mapHomeScreenFocusType = when (it.mapHomeScreenFocusType) {
                    is MapHomeScreenFocusType.MEMBER -> MapHomeScreenFocusType.MEMBER(
                        memberUserId = memberItem.memberState.userId,
                        memberId = memberItem.memberState.memberId,
                        memberLocationId = memberItem.singleLocation?.locationId,
                        memberKeyId = memberItem.keyId,
                        forceZoom = false
                    )

                    else -> it.mapHomeScreenFocusType
                },
                members = it.members.map {
                    if (it.keyId == memberItem.keyId) {
                        it.copy(
                            location = it.location?.copy(
                                visibility = it.location.visibility.show()
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

        val memberStateForMarker = _uiState.value.asReady()
            ?.members?.firstOrNull { it.keyId == markerData.markerId }

        if (marker != null) {
            updateReadyState { readyState ->
                readyState.copy(
                    trackingMarker = markerData.takeIf { !marker.hidden },
                    markers = readyState.markers.map {
                        it.copy(focused = it.markerId == markerData.markerId)
                    },
                    mapHomeScreenFocusType = when (markerData.type) {
                        MapPinType.MEMBER -> {
                            if (memberStateForMarker != null) {
                                MapHomeScreenFocusType.MEMBER(
                                    memberUserId = memberStateForMarker.memberState.userId,
                                    memberId = memberStateForMarker.memberState.memberId,
                                    memberLocationId = memberStateForMarker.location?.locationId,
                                    memberKeyId = markerData.markerId,
                                    forceZoom = isRequestForSameVisibleMarker
                                )
                            } else {
                                messageDispatcher.showSnackbar(MR.strings.group_member_no_location_info_message.localizedString())
                                readyState.mapHomeScreenFocusType
                            }
                        }

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
                it.copy(location = it.location?.copy(visibility = it.location.visibility.tryToHide()))
            },
            mapHomeScreenFocusType = MapHomeScreenFocusType.NONE
        )
    }

    fun setSelectedContentType(selectedContentType: MapSheetSelectedContentType) =
        updateReadyState {
            it.copy(
                trackingMarker = null,
                markers = it.markers.map { it.copy(focused = false, hidden = it.obsolete) },
                mapHomeScreenFocusType = when (selectedContentType) {
                    MapSheetSelectedContentType.PLACES -> MapHomeScreenFocusType.ALL_PLACES
                    MapSheetSelectedContentType.MEMBERS -> MapHomeScreenFocusType.ALL_MEMBERS
                },
                mapSheetSelectedContentType = selectedContentType
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

    private fun keepLocationFocusOn(
        focusType: MapHomeScreenFocusType
    ) {
        activePingSubscriptionJob?.cancel()

        val groupId = groupIdData.value

        if (groupId.isNullOrBlank()) {
            logg.v("location_performance", "Group id not valid, ignore active ping")
            return
        }

        if (!deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
            logg.v("location_performance", "App is in background, ignore active ping")
            return
        }

        logg.tag("location_performance").v("Map active for $groupId")
        activePingSubscriptionJob = locationRequestUseCase.keepLocationFocus(
            groupId = groupId,
            focusType = focusType
        )
            .onStart { logg.v("location_performance", "Group ready ($groupId), start active ping") }
            .launchIn(coroutineScope)
    }

    private fun initRealtime() {
        realtimeSubscriptionJob?.cancel()
        if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
            realtimeSubscriptionJob = realtimeEvents.listenForEventsWhileActive()
                .mapToPayload { Unit }
                .launchIn(coroutineScope)
        }
    }

    fun memberAction(action: MemberActionType, state: GroupMemberItemState) {
        when (action) {
            MemberActionType.DELETE_MEMBER,
            MemberActionType.BLOCK_USER,
            MemberActionType.UNBLOCK_USER -> {
            }

            MemberActionType.DELETE_LOCATION -> {
                if (state is GroupMemberItemStateWithOneLocation) {
                    clearMyLocation(state)
                }
            }
        }
    }

    private fun clearMyLocation(state: GroupMemberItemStateWithOneLocation) {
        if (state.location == null) {
            return
        }

        clearUserLocation.clearLocation(state.location.locationId)
            .onStart {
                updateReadyState { readyState ->
                    readyState.copy(
                        members = readyState.members.map {
                            if (it.keyId == state.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        deleteLocation = it.actions.deleteLocation?.copy(isLoading = true)
                                    )
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
                delay(500)
            }
            .onCompletion {
                updateReadyState { readyState ->
                    readyState.copy(
                        members = readyState.members.map { memberState ->
                            if (memberState.keyId == state.keyId) {
                                memberState.copy(
                                    actions = memberState.actions.copy(
                                        deleteLocation = memberState.actions.deleteLocation
                                            ?.takeIf { action ->
                                                canDeleteLocation(
                                                    groupId = memberState.memberState.groupId,
                                                    isCurrentUser = memberState.memberState.userId == config.userId,
                                                    isCurrentDevice = memberState.location?.currentDevice == true,
                                                    hasLocation = memberState.location?.visibility?.isVisible() == true
                                                )
                                            }
                                            ?.copy(isLoading = false)
                                    )
                                )
                            } else {
                                memberState
                            }
                        }
                    )
                }

                if (it != null) {
                    messageDispatcher.showSnackbar(it.errorString())
                }
            }
            .catch { /* errors are handled in onCompletion */ }
            .launchIn(coroutineScope)
    }

    fun requestLocationUpdate(state: GroupMemberItemState) {

        logg.v("location_request", "Got location request click: ${state::class}")
        if (state !is GroupMemberItemStateWithOneLocation) {
            logg.v("location_request", "Abort location request")
            return
        }

        if (state.location == null || state.actions.locationRequest == null) {
            coroutineScope.launch {
                messageDispatcher.showSnackbar(Throwable("Active device not found").errorString())
            }
            return
        }

        if (state.actions.locationRequest?.isEnabled == false) {
            coroutineScope.launch {
                messageDispatcher.showSnackbar(MR.strings.group_member_device_request_disabled.localizedString())
            }
            return
        }

        flow {
            locationRequestUseCase.requestMemberDeviceLocation(
                groupId = state.memberState.groupId,
                memberUserId = state.memberState.userId,
                locationId = state.location.locationId
            )
            delay(500)
            emit(Unit)
        }
            .onStart {
                updateReadyState { readyState ->
                    readyState.copy(
                        members = readyState.members.map {
                            if (it.keyId == state.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        locationRequest = LocationRequestItemState(
                                            isLoading = true,
                                            isWaitingForUser = false
                                        )
                                    )
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
            }
            .onCompletion { error ->
                val hasDeviceUnreachableError = error?.asRemoteException()?.code == 404
                val hasDeviceSetupWrongError = error?.asRemoteException()?.type.let {
                    it == "device_setup_invalid" || it == "push_message_failed"
                }
                updateReadyState { readyState ->
                    readyState.copy(
                        members = readyState.members.map {
                            if (it.keyId == state.keyId) {
                                it.copy(
                                    actions = it.actions.copy(
                                        locationRequest = it.actions.locationRequest?.copy(
                                            isLoading = error == null,
                                            isEnabled = true,
                                            isWaitingForUser = error == null,
                                            hasRequestError = error != null,
                                            hasDeviceUnreachableError = hasDeviceUnreachableError,
                                            hasDeviceSetupWrongError = hasDeviceSetupWrongError,
                                            lastLocationRequest = nowUTC()
                                        )
                                    )
                                )
                            } else {
                                it
                            }
                        }
                    )
                }

                if (error != null) {
                    // only show snackbar for other type of errors
                    // these are displayed on member item on map
                    if (!hasDeviceUnreachableError && !hasDeviceSetupWrongError) {
                        messageDispatcher.showSnackbar(error.errorString())
                    }
                } else {
                    scheduleDeviceResponseCheck()
                }
            }
            .catch { /* errors are handled in onCompletion */ }
            .launchIn(coroutineScope)
    }

    private fun scheduleDeviceResponseCheck() {
        coroutineScope.launch {
            delay(locationRequestDuration)
            val anyObsoleteRequests = _uiState.value.asReady()?.members?.any { member ->
                member.actions.locationRequest?.let { request ->
                    val isWaiting = request.isWaitingForUser
                    val isObsolete = request.lastLocationRequest?.plus(locationRequestDuration)
                        ?.let { it < nowUTC() } ?: true
                    isWaiting && isObsolete
                } ?: false
            } ?: false

            if (anyObsoleteRequests) {
                messageDispatcher.showSnackbar(
                    text = MR.strings.group_member_device_not_responding_message.localizedString(),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
}