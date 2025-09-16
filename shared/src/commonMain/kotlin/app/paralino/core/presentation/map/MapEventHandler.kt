package app.paralino.core.presentation.map

import app.paralino.core.db.AppPrefs
import app.paralino.core.models.base.asGroupOrSpaceId
import app.paralino.core.models.group.UserGroup
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.map.MapHomeScreenEvent.*
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenCreatePlace
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenExternalNavigation
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenPlaceDetails
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenPlaces
import app.paralino.core.presentation.map.MapHomeScreenEvent.OpenSharingStart
import app.paralino.core.ui.components.dialogs.MainAlertDialog
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
    private val uiState: StateFlow<MapHomeScreenState>,
    private val eventChannel: Channel<MapHomeScreenEvent?>,
    private val appPrefs: AppPrefs,
    private val uiFocusedStatus: MutableStateFlow<Boolean>,
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
                mapStateManager.switchMapMode { transform ->
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
                eventChannel.trySend(MapHomeScreenEvent.OpenGroupPicker)
            }

            MapHomeScreenUiEvent.OnOpenCreatePlaceScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(OpenCreatePlace(groupId = it.groupOrSpaceId.id))
                }
            }

            MapHomeScreenUiEvent.OnOpenPlacesScreen -> {
                uiState.value.asReady()?.let {
                    eventChannel.trySend(OpenPlaces(groupId = it.groupOrSpaceId.id))
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
                coroutineScope.launch {
                    groupStateManager.switchGroup(
                        MapScreenViewSourceType.DefaultGroup(
                            appPrefs.selectedGroupId?.asGroupOrSpaceId()
                        )
                    )
                }
            }

            MapHomeScreenUiEvent.SwitchToPersonalSpace -> {
                coroutineScope.launch {
                    groupStateManager.switchGroup(MapScreenViewSourceType.PersonalSpace)
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
            is GroupMemberItemEvent.onItemClick -> {
                mapStateManager.trackMarker(uiState, updateReadyState, event.item.asFocusedMarker())
            }

            is GroupMemberItemEvent.onLocationRequest -> {
                memberActionHandler.requestLocationUpdate(
                    state = event.item,
                    updateReadyState = updateReadyState,
                    uiState = uiState,
                    getAllGroupsItemState = {
                        groupStateManager.getGroupPickerState()
                            .let { it.createdGroups + it.joinedGroups }
                    }
                )
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
                        OpenExternalNavigation(
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
                    OpenPlaceDetails(
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
} 