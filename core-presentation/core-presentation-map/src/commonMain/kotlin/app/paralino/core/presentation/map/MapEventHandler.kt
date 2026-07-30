package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.domain.location.StopSharingLocationUseCase
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.models.map.MapHomeScreenFocusType
import app.paralino.core.models.map.MapHomeScreenFocusType.*
import app.paralino.core.models.settings.MapInteractionsType
import app.paralino.core.presentation.billing.BillingScreenSourceType
import app.paralino.core.presentation.common.AppSnackbarData
import app.paralino.core.presentation.common.GroupMemberItemEvent
import app.paralino.core.presentation.common.GroupMemberItemStateWithFullStateLocations
import app.paralino.core.presentation.common.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.common.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.common.SnackbarDuration
import app.paralino.core.presentation.common.onAction
import app.paralino.core.presentation.map.MapHomeScreenEvent.*
import app.paralino.core.presentation.map.MapScreenViewSourceType.DefaultGroup
import app.paralino.core.provider.IDeviceApiProvider
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapEventHandler(
    private val mapStateManager: MapStateManager,
    private val groupStateManager: GroupStateManager,
    private val memberActionHandler: MemberActionHandler,
    private val locationFocusManager: LocationFocusManager,
    private val groupEmptyStateManager: GroupsInitEmptyStateManager,
    private val deviceApiProvider: IDeviceApiProvider,
    private val uiState: StateFlow<MapHomeScreenState>,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
    private val appPrefs: AppPrefs,
    private val groupLocal: UserGroupLocalDataSource,
    private val stopSharingLocationUseCase: StopSharingLocationUseCase,
    private val uiFocusedStatus: MutableStateFlow<Boolean>,
    private val messageDispatcher: ICoreMessageDispatcher,
    private val updateUiState: (MapHomeScreenState) -> Unit,
    private val updateReadyState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
    private val coroutineScope: CoroutineScope
) {

    fun onEvent(event: MapHomeScreenUiEvent) {
        when (event) {
            is MapHomeScreenUiEvent.DialogAction -> {
                handleDialogAction(event)
            }

            is MapHomeScreenUiEvent.UiLifecycleChange -> {
                uiFocusedStatus.update { event.active }
            }

            is MapHomeScreenUiEvent.SwitchViewSource -> {
                coroutineScope.launch {
                    groupStateManager.switchGroup(event.viewSource)
                }
            }

            MapHomeScreenUiEvent.TryCreateGroup -> {
                groupStateManager.tryCreateGroup()
            }

            MapHomeScreenUiEvent.OnCloseFocusedItemClick -> {
                mapStateManager.hideFocusedItem(updateReadyState)
            }

            MapHomeScreenUiEvent.OnBackClicked -> {
                if (uiState.value.asReady()?.mapHomeScreenFocusType?.asLocationHistory != null) {
                    updateReadyState {
                        it.copy(
                            mapHomeScreenFocusType = it.mapHomeScreenFocusType.asLocationHistory
                                ?.member
                                ?: MapHomeScreenFocusType.NONE
                        )
                    }
                } else {
                    mapStateManager.hideFocusedItem(updateReadyState)
                }
            }

            MapHomeScreenUiEvent.OnFocusOnMembersClick -> {
                mapStateManager.setSelectedContentType(
                    updateState = updateReadyState,
                    selectedContentType = MapSheetSelectedContentType.MEMBERS
                )
            }

            MapHomeScreenUiEvent.OnFocusOnPlacesClick -> {
                mapStateManager.setSelectedContentType(
                    updateState = updateReadyState,
                    selectedContentType = MapSheetSelectedContentType.PLACES
                )
            }

            MapHomeScreenUiEvent.OnFocusOnPrivacyZonesClick -> {
                mapStateManager.setSelectedContentType(
                    updateState = updateReadyState,
                    selectedContentType = MapSheetSelectedContentType.PRIVACY_ZONES
                )
            }

            MapHomeScreenUiEvent.OnMapModeClick -> {
                mapStateManager.switchMapMode(uiState) { transform ->
                    updateUiState(transform(uiState.value))
                }
            }

            MapHomeScreenUiEvent.OnMapModeLongClick -> {
                when (uiState.value.deviceType) {
                    DeviceType.Android -> {
                        eventChannel.trySend(MapHomeScreenEvent.OpenMapProviderSettings)
                    }
                    DeviceType.iOS -> {}
                }
            }

            MapHomeScreenUiEvent.OnMapTouch -> {
                mapStateManager.onMapTouch(uiState, updateReadyState)
            }

            is MapHomeScreenUiEvent.OnMapLongPress -> {
                uiState.value.asReady()?.let { readyState ->
                    if (readyState.locationHistoryMode == null) {
                        updateReadyState {
                            it.copy(
                                tappedCoordinateState = MapTappedCoordinateState(
                                    lat = event.location.lat,
                                    lng = event.location.lng
                                )
                            )
                        }
                    }
                }
            }

            is MapHomeScreenUiEvent.OnMapClick -> {
                val readyState = uiState.value.asReady()
                if (readyState?.isMapFocusRequested == true) {
                    if (readyState.trackingMarker != null) {
                        // refocus back to same marker
                        mapStateManager.trackMarker(
                            uiState = uiState,
                            updateState = updateReadyState,
                            markerData = readyState.trackingMarker
                        )
                    } else if (readyState.mapHomeScreenFocusType.isMemberPlaceOrHistoryFocused) {
                        updateReadyState { state ->
                            state.copy(
                                isMapFocusRequested = false,
                                tappedCoordinateState = null
                            )
                        }
                    }
                } else {
                    mapStateManager.onMapTouch(uiState, updateReadyState)
                }
            }

            MapHomeScreenUiEvent.OnRefocusSelectedDeviceItem -> {
                updateReadyState { state ->
                    state.copy(isMapFocusRequested = false)
                }
            }

            is MapHomeScreenUiEvent.OnMarkerClick -> {
                mapStateManager.trackMarker(uiState, updateReadyState, event.pin.asFocusedMarker())
            }

            is MapHomeScreenUiEvent.OnMemberItemEvent -> {
                handleMemberItemEvent(event.event)
            }

            is MapHomeScreenUiEvent.OnMapSheetPositionChange -> {
                updateReadyState {
                    it.copy(
                        isMapFocusRequested = event.position is BottomSheetPosition.COLLAPSED_MINIMISED,
                        desiredMapSheetPosition = event.position.takeIf {
                            // do not accept hidden, since that is handled by different state
                            it.order < BottomSheetPosition.HIDDEN.order
                        }
                    )
                }
            }

            is MapHomeScreenUiEvent.OnScreenDensityChange -> {
                updateReadyState { state ->
                    state.copy(
                        mapInteractionType = MapInteractionsType.forScreenSize(
                            defaultType = appPrefs.mapInteractions,
                            maxWidthDp = event.maxWidthDp,
                            maxHeightDp = event.maxHeightDp
                        )
                    )
                }
            }

            MapHomeScreenUiEvent.OnGroupDetailsClick -> {
                uiState.value.asReady()?.let {
                    if (it.groupOrSpaceId.isPersonalSpace) {
                        eventChannel.trySend(MapHomeScreenEvent.OpenPersonalSpace)
                    } else {
                        eventChannel.trySend(OpenGroupDetails(groupId = it.groupOrSpaceId.id))
                    }
                }
            }

            MapHomeScreenUiEvent.OnGroupPickerClick -> {
                eventChannel.trySend(OpenGroupPicker(showPersonalSpace = false))
            }

            is MapHomeScreenUiEvent.OnOpenCreatePlaceScreen -> {
                updateReadyState { it.copy(tappedCoordinateState = null) }
                uiState.value.asReady()?.let {
                    eventChannel.trySend(
                        OpenCreatePlace(
                            groupId = it.groupOrSpaceId.id,
                            location = event.location
                        )
                    )
                }
            }

            is MapHomeScreenUiEvent.OnOpenCreatePrivacyZoneScreen -> {
                updateReadyState { it.copy(tappedCoordinateState = null) }
                uiState.value.asReady()?.let {
                    eventChannel.trySend(
                        OpenCreatePrivacyZone(
                            groupId = it.groupOrSpaceId.id,
                            location = event.location
                        )
                    )
                }
            }

            MapHomeScreenUiEvent.OnOpenPlacesScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(OpenPlaces(groupId = it.groupOrSpaceId.id))
                }
            }

            MapHomeScreenUiEvent.OnOpenPrivacyZonesScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(OpenPrivacyZones(groupId = it.groupOrSpaceId.id))
                }
            }

            MapHomeScreenUiEvent.OnOpenMemberInviteScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(OpenMemberInviteScreen(groupId = it.groupOrSpaceId.id))
                }
            }

            is MapHomeScreenUiEvent.OnPlaceClick -> {
                handlePlaceClick(event)
            }

            is MapHomeScreenUiEvent.OnPrivacyZoneClick -> {
                handlePrivacyZoneClick(event)
            }

            MapHomeScreenUiEvent.OnSettingsClick -> {
                eventChannel.trySend(MapHomeScreenEvent.OpenSettings)
            }

            MapHomeScreenUiEvent.OnSharingStartClick -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(OpenSharingStart(groupId = it.groupOrSpaceId.id))
                }
            }

            is MapHomeScreenUiEvent.OpenGroupDetails -> {
                if (event.groupId.isPersonalSpace) {
                    eventChannel.trySend(MapHomeScreenEvent.OpenPersonalSpace)
                } else {
                    eventChannel.trySend(OpenGroupDetails(groupId = event.groupId.id))
                }
            }

            MapHomeScreenUiEvent.SwitchToGroups -> {
                if (uiState.value.isPersonalSpace) {
                    coroutineScope.launch {
                        groupStateManager.switchGroup(
                            DefaultGroup(
                                appPrefs.selectedGroupId?.asGroupOrSpaceId()
                            )
                        )
                    }
                } else {
                    eventChannel.trySend(OpenGroupPicker(showPersonalSpace = false))
                }
            }

            MapHomeScreenUiEvent.SwitchToPersonalSpace -> {
                if (uiState.value.isPersonalSpace) {
                    eventChannel.trySend(OpenGroupPicker(showPersonalSpace = true))
                } else {
                    coroutineScope.launch {
                        groupStateManager.switchGroup(MapScreenViewSourceType.PersonalSpace)
                    }
                }
            }

            is MapHomeScreenUiEvent.OnGroupsEmptyStateEvent -> {
                groupEmptyStateManager.onEvent(event.event)
            }

            MapHomeScreenUiEvent.ConfirmPersonalSpaceIntro -> {
                appPrefs.didHavePersonalSpaceIntroOnboarding = true
                updateReadyState {
                    it.copy(isFirstTimeInPersonalSpace = false)
                }
            }

            MapHomeScreenUiEvent.OpenPersonalLocationHistory -> {
                updateReadyState {
                    it.copy(
                        mapHomeScreenFocusType = LOCATION_HISTORY(
                            member = it.mapHomeScreenFocusType.asMember,
                            groupId = UserGroup.groupPersonalDevicesId,
                            memberUserId = appPrefs.userId,
                            deviceId = null
                        )
                    )
                }
            }

            MapHomeScreenUiEvent.CloseLocationHistory -> {
                updateReadyState {
                    it.copy(
                        mapHomeScreenFocusType = it.mapHomeScreenFocusType.asLocationHistory
                            ?.member
                            ?: MapHomeScreenFocusType.NONE
                    )
                }
            }

            MapHomeScreenUiEvent.OnOverLimitsBannerTap -> {
                coroutineScope.launch {
                    showOverLimitInfoMessage()
                }
            }

            is MapHomeScreenUiEvent.OnConditionErrorItemClick -> {
                when (event.item) {
                    ConditionErrorType.GPS -> eventChannel.trySend(MapHomeScreenEvent.AskLocationServices)
                    ConditionErrorType.Network -> messageDispatcher.showSnackbar(MR.strings.error_no_connection_snackbar.localizedString())
                    is ConditionErrorType.PermissionLocation -> eventChannel.trySend(MapHomeScreenEvent.AskLocationPermission)
                    ConditionErrorType.PermissionLocationBackground -> eventChannel.trySend(MapHomeScreenEvent.AskBackgroundLocationPermission)
                    ConditionErrorType.PermissionNotifications -> eventChannel.trySend(MapHomeScreenEvent.AskNotificationPermission)
                    else -> {
                        // not handled in map
                    }
                }
            }

            is MapHomeScreenUiEvent.OpenPaywall -> {
                eventChannel.trySend(OpenPaywall(type = event.type))
            }

            is MapHomeScreenUiEvent.StopLocationSharing -> {
                coroutineScope.launch {
                    stopSharingLocationUseCase.stopSharing(groupOrSpaceId = event.groupOrSpaceId)
                }
            }
        }
    }

    private fun handleDialogAction(event: MapHomeScreenUiEvent.DialogAction) {
        when (event.action.dialog) {
            is MainAlertDialog.ServiceMaxLimit -> {
                updateUiState(
                    when (val currentState = uiState.value) {
                        is MapHomeScreenState.Empty -> currentState.copy(alertDialog = null)
                        is MapHomeScreenState.Ready -> currentState.copy(alertDialog = null)
                    }
                )
            }

            else -> {}
        }
    }

    private fun handleMemberItemEvent(event: GroupMemberItemEvent) {
        when (event) {
            is GroupMemberItemEvent.onActionClick -> {
                memberActionHandler.memberAction(
                    action = event.action.type,
                    state = event.item,
                    updateReadyState = updateReadyState,
                    uiState = uiState
                )
            }

            is GroupMemberItemEvent.onCopyCode -> {}
            is GroupMemberItemEvent.onDeleteDeviceClick -> {}

            is GroupMemberItemEvent.onDeviceClick -> {
                mapStateManager.trackMarker(uiState, updateReadyState, event.device.asFocusedMarker())
            }

            is GroupMemberItemEvent.onItemClick -> {
                if (event.item !is GroupMemberItemStateWithFullStateLocations) {
                    mapStateManager.trackMarker(uiState, updateReadyState, event.item.asFocusedMarker())
                }
            }

            is GroupMemberItemEvent.onLocationRequest -> {
                memberActionHandler.requestLocationUpdate(
                    state = event.item,
                    updateReadyState = updateReadyState,
                    getAllGroupsItemState = {
                        groupStateManager.getGroupPickerState(showPersonalSpace = false)
                            .let { it.createdGroups + it.joinedGroups }
                    }
                )
            }

            is GroupMemberItemEvent.onOpenLocationHistory -> {
                updateReadyState {
                    it.copy(
                        mapHomeScreenFocusType = MapHomeScreenFocusType.LOCATION_HISTORY(
                            member = it.mapHomeScreenFocusType.asMember,
                            groupId = event.item.memberState.groupId,
                            memberUserId = event.item.memberState.userId,
                            deviceId = when (val item = event.item) {
                                is GroupMemberItemStateWithLocations -> null
                                is GroupMemberItemStateWithFullStateLocations -> null
                                is GroupMemberItemStateWithOneLocation -> item.deviceId
                            }
                        )
                    )
                }
            }

            is GroupMemberItemEvent.onOpenRawData -> {}

            is GroupMemberItemEvent.onShowLocation -> {
                mapStateManager.toggleMarkerVisibility(
                    uiState = uiState,
                    updateState = updateReadyState,
                    updateRefreshPing = { locationFocusManager.updateRefreshPing() },
                    memberItem = event.item
                )
            }

            is GroupMemberItemEvent.onDirectionsRequest -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(
                        MapHomeScreenEvent.OpenExternalNavigation(
                            lat = event.action.lat,
                            lng = event.action.lng,
                            name = event.action.title,
                            navigationApp = appPrefs.iosNavigationApp
                        )
                    )
                }
            }

            is GroupMemberItemEvent.onCoordinateCopy -> {
                uiState.value.asReady()?.let {
                    deviceApiProvider.copyToClipboard(
                        data = "${event.action.lat},${event.action.lng}"
                    )
                }
            }
        }
    }

    private fun handlePlaceClick(event: MapHomeScreenUiEvent.OnPlaceClick) {
        uiState.value.asReady()?.let { readyState ->
            if (readyState.focusedPlace?.id == event.place.id) {
                eventChannel.trySend(
                    MapHomeScreenEvent.OpenPlaceDetails(
                        groupId = readyState.groupOrSpaceId.id,
                        placeId = event.place.id
                    )
                )
            } else {
                mapStateManager.trackMarker(
                    uiState,
                    updateReadyState,
                    event.place.asFocusedMarker()
                )
            }
        }
    }

    private fun handlePrivacyZoneClick(event: MapHomeScreenUiEvent.OnPrivacyZoneClick) {
        uiState.value.asReady()?.let { readyState ->
            if (readyState.focusedPrivacyZone?.id == event.zone.id) {
                eventChannel.trySend(
                    MapHomeScreenEvent.OpenPrivacyZoneDetails(
                        groupId = readyState.groupOrSpaceId.id,
                        zoneId = event.zone.id
                    )
                )
            } else {
                mapStateManager.trackMarker(
                    uiState = uiState,
                    updateState = updateReadyState,
                    markerData = event.zone.asFocusedMarker()
                )
            }
        }
    }

    private suspend fun showOverLimitInfoMessage() {
        if (uiState.value.isPersonalSpace) {
            messageDispatcher.showSnackbarForResult(
                data = AppSnackbarData(
                    text = MR.strings.plan_limit_overLimit_personalSpace_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever,
                    actionText = MR.strings.plans_banner_settings_action_small.localizedString()
                )
            )?.result?.onAction {
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall(type = BillingScreenSourceType.UPGRADE_DEFAULT))
            }
        } else {
            val group = groupLocal.allGroupsState.value.data?.firstOrNull {
                it.id == uiState.value.selectedGroupId
            }

            if (group?.group?.isCurrentUserOwner == true) {
                messageDispatcher.showSnackbarForResult(
                    data = AppSnackbarData(
                        text = MR.strings.plan_limit_overLimit_groupOwner_banner_title.localizedString(),
                        duration = SnackbarDuration.Forever,
                        actionText = MR.strings.plans_banner_settings_action_small.localizedString()
                    )
                )?.result?.onAction {
                    eventChannel.trySend(MapHomeScreenEvent.OpenPaywall(type = BillingScreenSourceType.UPGRADE_DEFAULT))
                }
            } else {
                messageDispatcher.showSnackbar(
                    text = MR.strings.plan_limit_overLimit_groupMember_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever
                )
            }
        }
    }
} 