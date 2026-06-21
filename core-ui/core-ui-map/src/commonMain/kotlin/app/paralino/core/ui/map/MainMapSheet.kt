package app.paralino.core.ui.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.map.BottomSheetPosition
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.ui.CoreUiState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainMapSheet(
    appState: CoreUiState,
    swipeableState: SwipeableSheetState,
    screenPaddingValues: PaddingValues,
    mapUiState: MapHomeScreenState,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
    screenContent: @Composable () -> Unit,
) {
    LaunchedEffect(mapUiState.defaultSheetPosition) {
        snapshotFlow { mapUiState.defaultSheetPosition }
            .distinctUntilChanged()
            .onEach { position ->
                if (position == BottomSheetPosition.HIDDEN) {
                    swipeableState.snapTo(position)
                } else {
                    swipeableState.animateTo(position)
                }
            }
            .collect()
    }

    LaunchedEffect(swipeableState.state.settledValue) {
//        delay(1500)
        onEvent(MapHomeScreenUiEvent.OnMapSheetPositionChange(swipeableState.state.settledValue))
    }

    var infoHeight by remember { mutableStateOf(150f) }
    SwipeableBottomSheet(
        enabled = mapUiState.isSheetEnabled,
        screenPaddingValues = screenPaddingValues,
        collapsedInfoHeightPixel = infoHeight,
        collapsedMinimizedInfoHeightPixel = with(LocalDensity.current) {
            screenPaddingValues.calculateBottomPadding()
                .plus(SwipeableSheetState.sheetBottomCollapsedPadding)
                .plus(SwipeableSheetState.sheetBottomCollapsedMinimizedPadding)
                .plus(MapHomeSheetContentSizes.dividerTopPadding.times(2) + MapHomeSheetContentSizes.dividerHeight.times(2))
                // this one is to compensate for the padding of sheet top divider/handler
                .plus(MapHomeSheetContentSizes.dividerHeight)
                .plus(MapHomeSheetContentSizes.collapsedDividerTopPadding)
                .toPx()
        },
        swipeableState = swipeableState,
        screenContent = screenContent,
        body = {
            when (mapUiState) {
                is MapHomeScreenState.Empty -> {
                    val groupsEmptyState = mapUiState.groupsEmptyState
                    if (groupsEmptyState != null) {
                        GroupsInitEmptyScreenContent(
                            modifier = Modifier,
                            paddingValues = PaddingValues(
                                bottom = screenPaddingValues.calculateTopPadding() + 40.dp
                            ),
                            state = groupsEmptyState,
                            onEvent = {
                                onEvent(MapHomeScreenUiEvent.OnGroupsEmptyStateEvent(it))
                            }
                        )
                    }
                }

                is MapHomeScreenState.Ready -> {
                    if (mapUiState.locationHistoryMode == null) {
                        MainMapSheetActiveBody(
                            screenPaddingValues = screenPaddingValues,
                            appState = appState,
                            swipeableState = swipeableState,
                            readyState = mapUiState,
                            onMapGroupInfoHeightChange = { infoHeight = it },
                            onEvent = onEvent
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun MainMapSheetActiveBody(
    modifier: Modifier = Modifier,
    screenPaddingValues: PaddingValues,
    appState: CoreUiState,
    swipeableState: SwipeableSheetState,
    readyState: MapHomeScreenState.Ready,
    onMapGroupInfoHeightChange: (Float) -> Unit,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    MapHomeSheetContent(
        modifier = modifier,
        screenPaddingValues = screenPaddingValues,
        swipeableState = swipeableState,
        readyState = readyState,
        members = readyState.membersWithDevices,
        places = readyState.places,
        privacyZones = readyState.privacyZones,
        onEvent = onEvent,
        onMapGroupInfoHeightChange = onMapGroupInfoHeightChange,
        onWholeContentClick = {
            if (readyState.isSheetEnabled) {
                appState.uiScope.launch {
                    if (swipeableState.isCollapsed() || swipeableState.isFullyExpanded()) {
                        swipeableState.animateTo(BottomSheetPosition.HALF_EXPANDED)
                    } else {
                        swipeableState.animateTo(BottomSheetPosition.COLLAPSED)
                    }
                }
            }
        }
    )
}