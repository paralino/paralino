package app.paralino.core.ui.map

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.ui.CoreUiState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainMapSheet(
    appState: CoreUiState,
    swipeableState: SwipeableState<BottomSheetPosition>,
    trackMarker: (FocusedMarkerData) -> Unit,
    readyState: MapHomeScreenState.Ready?,
    createAnchors: (screenHeightPixels: Float) -> Map<Float, BottomSheetPosition> = {
        BottomSheetPosition.swipeAnchors(
            screenHeightPixels = it,
            collapsedAnchorBottomPaddingPixels = 0f
        )
    },
    onOpenGroupScreen: () -> Unit,
    onOpenPlacesScreen: () -> Unit,
    onOpenCreatePlaceScreen: () -> Unit,
    onFocusOnMembersClick: () -> Unit,
    onFocusOnPlacesClick: () -> Unit,
    screenContent: @Composable () -> Unit,
) {
    SwipeableBottomSheet(
        swipeableState = swipeableState,
        screenContent = screenContent,
        createAnchors = createAnchors,
        body = {
            MainMapSheetActiveBody(
                appState = appState,
                swipeableState = swipeableState,
                trackMarker = trackMarker,
                readyState = readyState,
                onOpenPlacesScreen = onOpenPlacesScreen,
                onOpenCreatePlaceScreen = onOpenCreatePlaceScreen,
                onOpenGroupScreen = onOpenGroupScreen,
                onFocusOnMembersClick = onFocusOnMembersClick,
                onFocusOnPlacesClick = onFocusOnPlacesClick
            )
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainMapSheetActiveBody(
    appState: CoreUiState,
    swipeableState: SwipeableState<BottomSheetPosition>,
    trackMarker: (FocusedMarkerData) -> Unit,
    readyState: MapHomeScreenState.Ready?,
    onOpenGroupScreen: () -> Unit,
    onOpenPlacesScreen: () -> Unit,
    onOpenCreatePlaceScreen: () -> Unit,
    onFocusOnMembersClick: () -> Unit,
    onFocusOnPlacesClick: () -> Unit,
) {
    LaunchedEffect(readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused) {
        snapshotFlow { readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused }
            .distinctUntilChanged()
            .onEach {
                when (it) {
                    true -> {
                        swipeableState.animateTo(BottomSheetPosition.HIDDEN)
                    }

                    else -> {
                        // TODO use previous value?
                        swipeableState.animateTo(BottomSheetPosition.default)
                    }
                }
            }
            .collect()
    }

    MapHomeSheetContent(
        swipeableState = swipeableState,
        readyState = readyState,
        members = readyState?.members ?: emptyList(),
        places = readyState?.places ?: emptyList(),
        onGroupClick = onOpenGroupScreen,
        onMemberClick = {
            trackMarker(it.asFocusedMarker())
        },
        onPlaceClick = {
            trackMarker(it.asFocusedMarker())
        },
        onOpenPlaces = {
            onOpenPlacesScreen()
        },
        onCreatePlace = {
            onOpenCreatePlaceScreen()
        },
        onWholeContentClick = {
            appState.uiScope.launch {
                if (swipeableState.isCollapsed() || swipeableState.isFullyExpanded()) {
                    swipeableState.animateTo(BottomSheetPosition.HALF_EXPANDED)
                } else {
                    swipeableState.animateTo(BottomSheetPosition.COLLAPSED)
                }
            }
        },
        onFocusOnMembersClick = onFocusOnMembersClick,
        onFocusOnPlacesClick = onFocusOnPlacesClick,
    )
}
