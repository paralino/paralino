package app.paralino.core.ui.map

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
    readyState: MapHomeScreenState.Ready?,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
    screenContent: @Composable () -> Unit,
) {
    var infoHeight by remember { mutableStateOf(0f) }
    SwipeableBottomSheet(
        collapsedInfoHeightPixel = infoHeight,
        swipeableState = swipeableState,
        screenContent = screenContent,
        body = {
            MainMapSheetActiveBody(
                appState = appState,
                swipeableState = swipeableState,
                readyState = readyState,
                onMapGroupInfoHeightChange = { infoHeight = it },
                onEvent = onEvent
            )
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
