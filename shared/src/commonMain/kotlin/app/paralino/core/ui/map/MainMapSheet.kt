package app.paralino.core.ui.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
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
import app.paralino.core.ui.CoreUiState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainMapSheet(
    appState: CoreUiState,
    swipeableState: SwipeableSheetState,
    mapUiState: MapHomeScreenState,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
    screenContent: @Composable () -> Unit,
) {
    LaunchedEffect(mapUiState is MapHomeScreenState.Ready) {
        snapshotFlow { mapUiState is MapHomeScreenState.Ready }
            .distinctUntilChanged()
            .collectLatest { isReadyState ->
                if (isReadyState) {
                    swipeableState.animateTo(BottomSheetPosition.HALF_EXPANDED)
                } else {
                    swipeableState.animateTo(BottomSheetPosition.EXPANDED)
                }
            }
    }

    var infoHeight by remember { mutableStateOf(0f) }
    SwipeableBottomSheet(
        enabled = mapUiState is MapHomeScreenState.Ready,
        collapsedInfoHeightPixel = infoHeight,
        swipeableState = swipeableState,
        screenContent = screenContent,
        body = {
            if (mapUiState is MapHomeScreenState.Ready) {
                MainMapSheetActiveBody(
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
                        bottom = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 40.dp
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
    appState: CoreUiState,
    swipeableState: SwipeableSheetState,
    readyState: MapHomeScreenState.Ready?,
    onMapGroupInfoHeightChange: (Float) -> Unit,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
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
        modifier = modifier,
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
