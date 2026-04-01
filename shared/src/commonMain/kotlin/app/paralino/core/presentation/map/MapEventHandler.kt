package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.data
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemStateWithFullStateLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.map.MapScreenViewSourceType.DefaultGroup
import app.paralino.core.presentation.settings.generic.MapInteractionsType
import app.paralino.core.provider.localizedString
import app.paralino.core.repo.group.UserGroupLocalDataSource
import app.paralino.core.ui.components.AppSnackbarData
import app.paralino.core.ui.components.SnackbarDuration
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.onAction
import app.paralino.core.ui.map.BottomSheetPosition
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
    private val uiState: StateFlow<MapHomeScreenState>,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
    private val appPrefs: AppPrefs,
    private val groupLocal: UserGroupLocalDataSource,
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
                    updateReadyState,
                    MapSheetSelectedContentType.MEMBERS
                )
            }

            MapHomeScreenUiEvent.OnFocusOnPlacesClick -> {
                mapStateManager.setSelectedContentType(
                    updateReadyState,
                    MapSheetSelectedContentType.PLACES
                )
            }

            MapHomeScreenUiEvent.OnMapModeSwitch -> {
                mapStateManager.switchMapMode(uiState) { transform ->
                    updateUiState(transform(uiState.value))
                }
            }

            MapHomeScreenUiEvent.OnMapTouch -> {
                mapStateManager.onMapTouch(uiState, updateReadyState)
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
                    val interaction = when (val interaction = appPrefs.mapInteractions) {
                        MapInteractionsType.DEVICE_DEFAULT -> {
                            when {
                                event.maxHeightDp.value < 460 -> {
                                    MapInteractionsType.COLLAPSED
                                }

                                event.maxHeightDp.value < 720 -> {
                                    MapInteractionsType.MINIMIZE
                                }

                                else -> {
                                    MapInteractionsType.NONE
                                }
                            }
                        }

                        else -> interaction
                    }

                    state.copy(mapInteractionType = interaction)
                }
            }

            MapHomeScreenUiEvent.OnGroupDetailsClick -> {
                uiState.value.asReady()?.let {
                    if (it.groupOrSpaceId.isPersonalSpace) {
                        eventChannel.trySend(MapHomeScreenEvent.OpenPersonalSpace)
                    } else {
                        eventChannel.trySend(MapHomeScreenEvent.OpenGroupDetails(groupId = it.groupOrSpaceId.id))
                    }
                }
            }

            MapHomeScreenUiEvent.OnGroupPickerClick -> {
                eventChannel.trySend(MapHomeScreenEvent.OpenGroupPicker(showPersonalSpace = false))
            }

            is MapHomeScreenUiEvent.OnOpenCreatePlaceScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(
                        MapHomeScreenEvent.OpenCreatePlace(
                            groupId = it.groupOrSpaceId.id,
                            location = event.location
                        )
                    )
                }
            }

            MapHomeScreenUiEvent.OnOpenPlacesScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(MapHomeScreenEvent.OpenPlaces(groupId = it.groupOrSpaceId.id))
                }
            }

            MapHomeScreenUiEvent.OnOpenMemberInviteScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(MapHomeScreenEvent.OpenMemberInviteScreen(groupId = it.groupOrSpaceId.id))
                }
            }

            is MapHomeScreenUiEvent.OnPlaceClick -> {
                handlePlaceClick(event)
            }

            MapHomeScreenUiEvent.OnSettingsClick -> {
                eventChannel.trySend(MapHomeScreenEvent.OpenSettings)
            }

            MapHomeScreenUiEvent.OnSharingStartClick -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(MapHomeScreenEvent.OpenSharingStart(groupId = it.groupOrSpaceId.id))
                }
            }

            is MapHomeScreenUiEvent.OpenGroupDetails -> {
                if (event.groupId.isPersonalSpace) {
                    eventChannel.trySend(MapHomeScreenEvent.OpenPersonalSpace)
                } else {
                    eventChannel.trySend(MapHomeScreenEvent.OpenGroupDetails(groupId = event.groupId.id))
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
                    eventChannel.trySend(MapHomeScreenEvent.OpenGroupPicker(showPersonalSpace = false))
                }
            }

            MapHomeScreenUiEvent.SwitchToPersonalSpace -> {
                if (uiState.value.isPersonalSpace) {
                    eventChannel.trySend(MapHomeScreenEvent.OpenGroupPicker(showPersonalSpace = true))
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
                        mapHomeScreenFocusType = MapHomeScreenFocusType.LOCATION_HISTORY(
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
                            deviceId = when (event.item) {
                                is GroupMemberItemStateWithLocations -> null
                                is GroupMemberItemStateWithFullStateLocations -> null
                                is GroupMemberItemStateWithOneLocation -> event.item.deviceId
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

    private suspend fun showOverLimitInfoMessage() {
        if (uiState.value.isPersonalSpace) {
            messageDispatcher.showSnackbarForResult(
                data = AppSnackbarData(
                    text = MR.strings.plan_limit_overLimit_personalSpace_banner_title.localizedString(),
                    duration = SnackbarDuration.Forever,
                    actionText = MR.strings.plans_banner_settings_action_small.localizedString()
                )
            )?.result?.onAction {
                eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
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
                    eventChannel.trySend(MapHomeScreenEvent.OpenPaywall)
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