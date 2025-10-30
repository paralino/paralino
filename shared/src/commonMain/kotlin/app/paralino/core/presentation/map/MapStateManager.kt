package app.paralino.core.presentation.map

import app.paralino.core.MR
import app.paralino.core.presentation.common.ICoreMessageDispatcher
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.provider.localizedString
import kotlinx.coroutines.flow.StateFlow

class MapStateManager(
    private val messageDispatcher: ICoreMessageDispatcher
) {
    
    fun trackMarker(
        uiState: StateFlow<MapHomeScreenState>,
        updateState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        markerData: FocusedMarkerData
    ) {
        val marker = uiState.value.asReady()
            ?.markers?.firstOrNull { it.markerId == markerData.markerId }

        val isRequestForSameVisibleMarker = uiState.value.asReady()
            ?.trackingMarker?.markerId == markerData.markerId

        val memberStateForMarker = uiState.value.asReady()
            ?.allMembersOrDevices?.firstOrNull { it.keyId == markerData.markerId }

        updateState { readyState ->
            readyState.copy(
                trackingMarker = markerData.takeIf { marker?.hidden == false },
                markers = readyState.markers.map {
                    it.copy(focused = it.markerId == markerData.markerId)
                },
                mapHomeScreenFocusType = when (markerData.type) {
                    MapPinType.MEMBER -> {
                        if (memberStateForMarker != null) {
                            MapHomeScreenFocusType.MEMBER(
                                memberUserId = memberStateForMarker.memberState.userId,
                                memberId = memberStateForMarker.memberState.memberId,
                                memberLocationId = memberStateForMarker.singleLocation?.locationId,
                                memberKeyId = markerData.markerId,
                                isIdleActive = memberStateForMarker.singleLocation?.deviceStatus?.isIdlingOrActive ?: false,
                                forceZoom = isRequestForSameVisibleMarker
                            )
                        } else {
                            readyState.mapHomeScreenFocusType
                        }
                    }

                    MapPinType.PLACE -> MapHomeScreenFocusType.PLACE(placeId = markerData.markerId)
                }
            )
        }

        if (marker == null || (markerData.type == MapPinType.MEMBER && memberStateForMarker?.singleLocation == null)) {
            messageDispatcher.showSnackbar(MR.strings.group_member_no_location_info_message.localizedString())
        }
    }

    fun onMapTouch(
        uiState: StateFlow<MapHomeScreenState>,
        updateState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
    ) {
        updateState { currentState ->
            if (currentState.mapHomeScreenFocusType.isMemberOrPlaceFocused) {
                // de-init focused marker so it is not tracked
                // and reset zoom enforcing for member focus
                currentState.copy(
                    trackingMarker = null,
                    mapHomeScreenFocusType = when (currentState.mapHomeScreenFocusType) {
                        is MapHomeScreenFocusType.MEMBER -> currentState.mapHomeScreenFocusType.copy(forceZoom = false)
                        else -> currentState.mapHomeScreenFocusType
                    }
                )
            } else {
                currentState.copy(
                    trackingMarker = null,
                    markers = currentState.markers.map { it.copy(focused = false, hidden = it.obsolete) },
                    mapHomeScreenFocusType = MapHomeScreenFocusType.NONE
                )
            }
        }
    }

    fun hideFocusedItem(
        updateState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
    ) {
        updateState {
            it.copy(
                trackingMarker = null,
                markers = it.markers.map { it.copy(focused = false, hidden = it.obsolete) },
                allMembersOrDevices = it.allMembersOrDevices.map {
                    it.copy(singleLocation = it.singleLocation?.copy(visibility = it.singleLocation.visibility.tryToHide()))
                },
                mapHomeScreenFocusType = MapHomeScreenFocusType.NONE
            )
        }
    }

    fun setSelectedContentType(
        updateState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        selectedContentType: MapSheetSelectedContentType
    ) {
        updateState { currentState ->
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
    }

    fun switchMapMode(
        updateState: ((MapHomeScreenState) -> MapHomeScreenState) -> Unit,
    ) {
        updateState { state ->
            when (state) {
                is MapHomeScreenState.Empty -> state.copy(isSatelliteMode = !state.isSatelliteMode)
                is MapHomeScreenState.Ready -> state.copy(isSatelliteMode = !state.isSatelliteMode)
            }
        }
    }

    fun toggleMarkerVisibility(
        uiState: StateFlow<MapHomeScreenState>,
        updateState: ((MapHomeScreenState.Ready) -> MapHomeScreenState.Ready) -> Unit,
        updateRefreshPing: () -> Unit,
        memberItem: GroupMemberItemState
    ) {
        updateState {
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
                        isIdleActive = memberItem.singleLocation?.deviceStatus?.isIdlingOrActive ?: false,
                        forceZoom = false
                    )

                    else -> it.mapHomeScreenFocusType
                },
                allMembersOrDevices = it.allMembersOrDevices.map {
                    if (it.keyId == memberItem.keyId) {
                        it.copy(
                            singleLocation = it.singleLocation?.copy(
                                visibility = it.singleLocation.visibility.show()
                            )
                        )
                    } else {
                        it
                    }
                }
            )
        }
        updateRefreshPing()
    }
} 