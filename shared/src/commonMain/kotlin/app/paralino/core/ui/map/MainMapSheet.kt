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
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.asReady
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
    LaunchedEffect(mapUiState.asReady()?.mapHomeScreenFocusType?.isMemberOrPlaceFocused) {
        snapshotFlow { mapUiState.asReady()?.mapHomeScreenFocusType?.isMemberOrPlaceFocused }
            .distinctUntilChanged()
            .onEach {
                when (it) {
                    true -> {
                        swipeableState.animateTo(BottomSheetPosition.HIDDEN)
                    }

                    false -> {
                        // TODO use previous value?
                        swipeableState.animateTo(BottomSheetPosition.HALF_EXPANDED)
                    }

                    else -> {
                        swipeableState.animateTo(BottomSheetPosition.EXPANDED)
                    }
                }
            }
            .collect()
    }

    var infoHeight by remember { mutableStateOf(0f) }
    SwipeableBottomSheet(
        enabled = mapUiState is MapHomeScreenState.Ready,
        screenPaddingValues = screenPaddingValues,
        collapsedInfoHeightPixel = infoHeight,
        swipeableState = swipeableState,
        screenContent = screenContent,
        body = {
            if (mapUiState is MapHomeScreenState.Ready) {
                MainMapSheetActiveBody(
                    screenPaddingValues = screenPaddingValues,
                    appState = appState,
                    swipeableState = swipeableState,
                    readyState = mapUiState,
                    onMapGroupInfoHeightChange = { infoHeight = it },
                    onEvent = onEvent
                )
            } else if (mapUiState is MapHomeScreenState.Empty && mapUiState.groupsEmptyState != null) {
                GroupsInitEmptyScreenContent(
                    modifier = Modifier,
                    paddingValues = PaddingValues(
                        bottom = screenPaddingValues.calculateTopPadding() + 40.dp
                    ),
                    state = mapUiState.groupsEmptyState,
                    onEvent = {
                        onEvent(MapHomeScreenUiEvent.OnGroupsEmptyStateEvent(it))
                    }
                )
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
    readyState: MapHomeScreenState.Ready?,
    onMapGroupInfoHeightChange: (Float) -> Unit,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    MapHomeSheetContent(
        modifier = modifier,
        screenPaddingValues = screenPaddingValues,
        swipeableState = swipeableState,
        readyState = readyState,
        members = readyState?.members ?: emptyList(),
        places = readyState?.places ?: emptyList(),
        onEvent = onEvent,
        onMapGroupInfoHeightChange = onMapGroupInfoHeightChange,
        onWholeContentClick = {
            appState.uiScope.launch {
                if (swipeableState.isCollapsed() || swipeableState.isFullyExpanded()) {
                    swipeableState.animateTo(BottomSheetPosition.HALF_EXPANDED)
                } else {
                    swipeableState.animateTo(BottomSheetPosition.COLLAPSED)
                }
            }
        }
    )
}
