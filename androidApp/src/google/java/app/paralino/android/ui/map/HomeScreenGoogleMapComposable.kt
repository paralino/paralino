package app.paralino.android.ui.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.history.LocationHistoryMapContent
import app.paralino.android.ui.history.rememberLocationHistoryMapController
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import com.google.maps.android.compose.rememberCameraPositionState


@Composable
fun HomeScreenGoogleMapComposable(
    modifier: Modifier = Modifier,
    uiState: MapHomeScreenState,
    readyState: MapHomeScreenState.Ready?,
    historyUiState: LocationHistoryScreenUiState?,
    mapContentPadding: PaddingValues,
    onMainEvent: (MapHomeScreenUiEvent) -> Unit,
    onHistoryEvent: (LocationHistoryUiEvent) -> Unit
) {
    var isMapLoaded by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()
    val homeMapController = rememberMainHomeMapController(
        uiState = uiState,
        mapContentPadding = mapContentPadding,
        isMapLoaded = isMapLoaded,
        cameraPositionState = cameraPositionState,
        onMapTouch = {
            onMainEvent(MapHomeScreenUiEvent.OnMapTouch)
        }
    )

    val mapController = if (historyUiState != null) {
        rememberLocationHistoryMapController(
            uiState = historyUiState,
            mapContentPadding = mapContentPadding,
            cameraPositionState = cameraPositionState,
            animateFirstContent = true,
            isSatelliteMode = uiState.isSatelliteMode,
            onMapTouch = {
                onHistoryEvent(LocationHistoryUiEvent.OnMapTouch)
            }
        )
    } else {
        homeMapController
    }

    GenericGoogleMapWrapper(
        modifier = modifier,
        mapData = mapController,
        onMapClick = {
            if (historyUiState != null) {
                onHistoryEvent(LocationHistoryUiEvent.OnMapTouch)
            } else {
                onMainEvent(MapHomeScreenUiEvent.OnMapTouch)
            }
        },
        onMapLongClick = {
            onMainEvent(MapHomeScreenUiEvent.OnOpenCreatePlaceScreen(it))
        },
        onMapLoaded = {
            isMapLoaded = true
        },
        content = {
            if (historyUiState != null) {
                LocationHistoryMapContent(
                    uiState = historyUiState,
                    isSatelliteMode = uiState.isSatelliteMode,
                    onEvent = onHistoryEvent
                )
            } else {
                MainHomeMapContent(
                    uiState = readyState,
                    onEvent = onMainEvent
                )
            }
        }
    )
}