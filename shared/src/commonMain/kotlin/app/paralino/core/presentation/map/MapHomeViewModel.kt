package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.app_rating.AppRatingManager
import app.paralino.core.app_rating.AppRatingSource
import app.paralino.core.db.AppPrefs
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
import app.paralino.core.repo.base.RemoteConfigRepository
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.repo.places.PlacesRepository
import app.paralino.core.repo.user.UserRepository
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.onAction
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.exceptions.asRemoteException
import app.paralino.core.utils.extensions.errorString
import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.mapToPayload
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MapHomeViewModel constructor(
    private val sharingManager: LocationSharingManager,
    private val groupRepo: UserGroupLocalDataSource,
    private val placesRepo: PlacesRepository,
    private val groupPreRefresh: GroupPreRefreshUseCase,
    private val locationRequestUseCase: LocationRequestUseCase,
    private val clearUserLocation: ClearUserLocationUseCase,
    private val realtimeEvents: RealtimeEventsMaker,
    private val userRepository: UserRepository,
    private val appPrefs: AppPrefs,
    private val deviceApiProvider: IDeviceApiProvider,
    private val valueFormatter: IValueFormatter,
    private val remoteConfig: RemoteConfigRepository,
    private val appRatingManager: AppRatingManager
) : CoreViewModel() {

    private val _uiState = MutableStateFlow<MapHomeScreenState>(
        MapHomeScreenState.Empty(
            currentUserImage = userRepository.localUser.value.data?.data?.userImage ?: ImageModel.emoji(Emojis.robot)
        )
    )
    val uiState: StateFlow<MapHomeScreenState> = _uiState

    private val _eventChannel = Channel<MapHomeScreenEvent?>()
    val events = _eventChannel.receiveAsFlow()

    private val uiFocusedStatus = MutableStateFlow(false)
    private val groupIdData = MutableStateFlow("")
    private val uiRefreshPingFlow = MutableStateFlow<Instant?>(null)

    private var uiRefreshPingSubscriptionJob: Job? = null
    private var activePingSubscriptionJob: Job? = null
    private var activeLocationBoostJob: Job? = null
    private var realtimeSubscriptionJob: Job? = null
    private var groupSubscriptionJob: Job = SupervisorJob()
    private val locationRequestDuration = 20.seconds
    private val locationRequestDelay = 1.minutes


    val allGroups: StateFlow<Resource<List<UserGroupWithMetadata>>> =
        groupRepo.allGroupsState

    init {
        appRatingManager.tryShowingAppRating(source = AppRatingSource.MAIN_MAP)

        allGroups.onEach { groupsData ->
            val selectedGroup = groupIdData.value.takeIf { it.isNotBlank() }
                ?: appPrefs.selectedGroupId
            if (groupsData is Resource.Ready && groupsData.data.isNotEmpty() && !selectedGroup.isNullOrBlank()) {
                if (groupsData.data.none { it.id == selectedGroup }) {
                    switchGroup(groupsData.data.firstOrNull()?.id ?: "")
                }
            }
        }.launchIn(coroutineScope)

        combine(
            deviceApiProvider.appLifecycleState,
            uiFocusedStatus
        ) { appLifecycleState, uiFocused ->
            keepUiRefreshPing()
            if (appLifecycleState.comingFromBackground && uiFocused) {
                if (groupIdData.value.isNotBlank()) {
                    delay(2000)
                    logg.v("home_screen", "App in foreground, try to refresh selected group ${groupIdData.value}")
                    coroutineScope.launch(Dispatchers.IO) {
                        groupPreRefresh.refreshGroupWithMembers(groupIdData.value)
                    }
                } else {
                    logg.v("home_screen", "App in foreground but no selected group - try to select default group")
                    switchGroup(groupId = "")
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        combine(
            groupIdData,
            deviceApiProvider.appLifecycleState
                .map { it.current.activeOrInactive }
                .distinctUntilChanged(),
            uiFocusedStatus,
            uiState.map { it.asReady()?.mapHomeScreenFocusType }
                .filterNotNull()
                .distinctUntilChanged(),
        ) { groupId, appState, isUiFocused, mapFocusType ->
            MapScreenLifecycleData(
                groupId = groupId,
                isAppFocused = appState,
                isUiFocused = isUiFocused,
                mapFocusType = mapFocusType
            )
        }
            .debounce(2000)
            .distinctUntilChanged()
            .onEach {
                initRealtime()
                keepLocationFocusOn(focusType = it.mapFocusType)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        groupIdData
            .onEach {
                if (it.isNotBlank()) {
                    logg.v("home_screen", "Selected group changed, try to refresh group $it")
                    coroutineScope.launch(Dispatchers.IO) {
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
                        uiRefreshPingFlow.filterNotNull(),
                        groupRepo.getGroupWithMembers(it),
                        placesRepo.getPlacesForGroupLocally(it),
                        getSharingContract(),
                        userRepository.localUser.map { it.data?.data }.distinctUntilChanged()
                    ) { _, group, places, sharingStatus, userData ->

                        if (group == null || userData == null) {
                            return@combine MapHomeScreenState.Empty(currentUserImage = ImageModel.emoji(Emojis.robot))
                        }

                        val groupPlanConfig = group.group.planConfig(remoteConfig.getConfigOnce())

                        val membersData = group.members.flatMap { member ->
                            if (member.locations.isEmpty()) {
                                listOf(
                                    GroupMemberItemStateWithOneLocation(
                                        memberState = GroupMemberState(
                                            member = member.member,
                                            isCurrentUser = member.member.userId == appPrefs.userId,
                                            canHaveExpandButton = false
                                        ),
                                        actions = MemberActionsState(),
                                        location = null
                                    )
                                )
                            } else {
                                val memberState = GroupMemberState(
                                    member = member.member,
                                    isCurrentUser = member.member.userId == appPrefs.userId,
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
                                                    isCurrentUser = member.member.userId == appPrefs.userId,
                                                    isCurrentDevice = location.deviceId == appPrefs.deviceId,
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
                                                        lastLocationRequest = previousState?.actions?.locationRequest?.lastLocationRequest,
                                                        lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant(),
                                                        isPremiumFeatureEnabled = groupPlanConfig.planTypeEnum.isPremium
                                                    )
                                                } else if (isRequestExpired && !isRequestInProgress) {
                                                    LocationRequestItemState(
                                                        isEnabled = canRequestLocationAgain,
                                                        lastLocationRequest = previousState?.actions?.locationRequest?.lastLocationRequest,
                                                        lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant(),
                                                        isPremiumFeatureEnabled = groupPlanConfig.planTypeEnum.isPremium
                                                    )
                                                } else {
                                                    previousState?.actions?.locationRequest?.copy(
                                                        lastDeviceUpdate = location.status.data?.timeUTC?.fromUTCtoInstant()
                                                    )
                                                }
                                            }
                                        ),
                                        location = GroupMemberItemLocationState(
                                            memberLocation = location,
                                            name = location.name,
                                            image = member.member.image,
                                            canEdit = false,
                                            currentDevice = location.deviceId == appPrefs.deviceId,
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
                            alertDialog = _uiState.value.alertDialog,
                            isSatelliteMode = _uiState.value.isSatelliteMode,
                            groupPlanConfig = groupPlanConfig
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
        coroutineScope.launch {
            val allGroups = allGroups.mapNotNull { it.data }
                .firstOrNull()
                ?: groupRepo.getAllGroupsWithMetadata().firstOrNull()
                ?: emptyList()

            val newGroup = allGroups.firstOrNull { it.id == groupId }
                ?: allGroups.firstOrNull { it.id == appPrefs.selectedGroupId }
                ?: allGroups.firstOrNull()

            val newGroupId = newGroup?.id ?: ""

            appPrefs.selectedGroupId = newGroupId
            groupIdData.update { newGroupId }

            if (newGroup != null && newGroup.isGroupOverLimits(remoteConfig.getConfigOnce())) {
                if (newGroup.group.isCurrentUserOwner) {
                    showSnackbar(
                        text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                        duration = SnackbarDuration.Forever
                    )
                    delay(1000)
                    _eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
                } else {
                    showSnackbar(
                        text = MR.strings.plan_limit_overLimit_groupMember_banner_title.localizedString(),
                        duration = SnackbarDuration.Forever
                    )
                }
            }
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
        updateReadyState { currentState ->
            currentState.copy(
                trackingMarker = null,
                markers = currentState.markers.map { it.copy(focused = false, hidden = it.obsolete) },
                mapHomeScreenFocusType = if (currentState.mapSheetSelectedContentType == selectedContentType) {
                    // refocus only if tapping again
                    when (selectedContentType) {
                        MapSheetSelectedContentType.PLACES -> MapHomeScreenFocusType.ALL_PLACES
                        MapSheetSelectedContentType.MEMBERS -> MapHomeScreenFocusType.ALL_MEMBERS
                    }
                } else {
                    currentState.mapHomeScreenFocusType
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

    fun getGroupPickerState(): BottomSheetPickerType.GroupPickerBottomSheet {
        val allGroups = getAllGroupsItemState()
        return BottomSheetPickerType.GroupPickerBottomSheet(
            onlyShowGroups = false,
            userPlan = userRepository.userPlans.value.highestValidPlanConfig,
            groupsLimit = userRepository.userPlans.value.groupsLimit,
            createdGroups = allGroups.filter { it.isCurrentUserOwner },
            joinedGroups = allGroups.filter { !it.isCurrentUserOwner }
        )
    }

    private fun getAllGroupsItemState(): List<GroupItemState> {
        return allGroups.value.data?.map { group ->
            val uiType = when {
                !group.isVerified -> GroupItemState.GroupUiType.UNVERIFIED
                group.isGroupOverLimits(remoteConfig.getCachedConfigOnce()) -> GroupItemState.GroupUiType.OVER_LIMITS
                sharingManager.isSharingActiveForGroup(group.id) -> GroupItemState.GroupUiType.ACTIVE
                else -> GroupItemState.GroupUiType.INACTIVE
            }
            val isSelected = group.id == groupIdData.value
            GroupItemState(
                group = group,
                config = remoteConfig.getCachedConfigOnce(),
                uiType = uiType,
                canEdit = true,
                isSelected = isSelected,
                isLarge = isSelected,
            )
        } ?: emptyList()
    }

    private fun keepUiRefreshPing() {
        uiRefreshPingSubscriptionJob?.cancel()
        if (deviceApiProvider.appLifecycleState.value.current.activeOrInactive && uiFocusedStatus.value) {
            uiRefreshPingSubscriptionJob = coroutineScope.launch(Dispatchers.IO) {
                while (currentCoroutineContext().isActive) {
                    uiRefreshPingFlow.update { nowUTC() }
                    delay(5.seconds)
                }
            }
        }
    }

    private fun keepLocationFocusOn(
        focusType: MapHomeScreenFocusType
    ) {
        activePingSubscriptionJob?.cancel()
        activeLocationBoostJob?.cancel()

        val groupId = groupIdData.value

        if (groupId.isNullOrBlank()) {
            logg.v("location_performance", "Group id not valid, ignore active ping")
            return
        }

        if (!deviceApiProvider.appLifecycleState.value.current.activeOrInactive) {
            logg.v("location_performance", "App is in background, ignore active ping")
            return
        }

        activeLocationBoostJob = locationRequestUseCase.keepLocationBoost(
            groupId = groupId
        )
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)

        if (!uiFocusedStatus.value) {
            logg.v("location_performance", "Map Ui is not focused, ignore active ping")
            return
        }

        activePingSubscriptionJob = locationRequestUseCase.keepLocationFocus(
            groupId = groupId,
            focusType = focusType
        )
            .flowOn(Dispatchers.IO)
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

        clearUserLocation.clearLocation(groupId = state.memberState.groupId, locationId = state.location.locationId)
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
                                                    isCurrentUser = memberState.memberState.userId == appPrefs.userId,
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

        if (state.actions.locationRequest?.isPremiumFeatureEnabled == false) {
            coroutineScope.launch {
                val group = getAllGroupsItemState().firstOrNull { it.id == state.memberState.groupId }
                if (group != null) {
                    if (group.isCurrentUserOwner) {
                        messageDispatcher.showSnackbarForResult(
                            data = AppSnackbarData(
                                text = MR.strings.plan_limit_locationRequest_groupOwner_message.localizedString(),
                                actionText = MR.strings.button_continue.localizedString(),
                                duration = SnackbarDuration.Long
                            )
                        )?.result?.onAction {
                            _eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
                        }
                    } else {
                        messageDispatcher.showSnackbar(MR.strings.plan_limit_locationRequest_groupMember_message.localizedString())
                    }
                }
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
                                            isWaitingForUser = false,
                                            isPremiumFeatureEnabled = readyState.groupPlanConfig.planTypeEnum.isPremium
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
                                            lastLocationRequest = nowUTC(),
                                            isPremiumFeatureEnabled = readyState.groupPlanConfig.planTypeEnum.isPremium
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
                        if (error.asRemoteException()?.code == 429) {
                            messageDispatcher.showSnackbar(MR.strings.group_member_device_request_disabled.localizedString())
                        } else {
                            messageDispatcher.showSnackbar(error.errorString())
                        }
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

    fun tryCreateGroup() {
        coroutineScope.launch {
            val limit = userRepository.userPlans.value.groupsLimit
            val groupCount = getAllGroupsItemState().filter { it.isCurrentUserOwner }.size
            val maxGroupLimit = remoteConfig.getConfigOnce().plans.maxByOrNull { it.planTypeEnum.order }?.groupsLimit
            val shouldShowFreePlanNoticeScreen = userRepository.shouldShowFreePlanNoticeScreen

            if (maxGroupLimit != null && groupCount >= maxGroupLimit) {
                updateReadyState {
                    it.copy(
                        alertDialog = MainAlertDialog.PlanLimit(
                            MR.strings.plan_limit_groups_alert_info.localizedString(),
                            limit = limit
                        )
                    )
                }
            } else if (limit in 0..groupCount) {
                _eventChannel.send(MapHomeScreenEvent.OpenPaywall)
            } else if (shouldShowFreePlanNoticeScreen) {
                _eventChannel.send(MapHomeScreenEvent.ShowFreePlanInfo)
            } else {
                _eventChannel.send(MapHomeScreenEvent.AddGroup)
            }
        }
    }

    fun onEvent(event: MapHomeScreenUiEvent) {
        when (event) {
            is MapHomeScreenUiEvent.DialogAction -> {
                when (event.action.dialog) {
                    is MainAlertDialog.PlanLimit -> {
                        _uiState.update {
                            when (it) {
                                is MapHomeScreenState.Empty -> it.copy(alertDialog = null)
                                is MapHomeScreenState.Ready -> it.copy(alertDialog = null)
                            }
                        }
                    }

                    else -> {}
                }
            }
            is MapHomeScreenUiEvent.UiLifecycleChange -> {
                uiFocusedStatus.update { event.active }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupSubscriptionJob.cancel()
    }
}