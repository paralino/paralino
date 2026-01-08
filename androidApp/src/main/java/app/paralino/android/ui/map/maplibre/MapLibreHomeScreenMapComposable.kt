package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.history.GoogleMapLocationHistoryMapContent
import app.paralino.android.ui.map.GoogleMapMainHomeMapContent
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent


@Composable
fun HomeScreenMapLibreMapComposable(
    modifier: Modifier = Modifier,
    uiState: MapHomeScreenState,
    readyState: MapHomeScreenState.Ready?,
    historyUiState: LocationHistoryScreenUiState?,
    mapContentPadding: PaddingValues,
    onMainEvent: (MapHomeScreenUiEvent) -> Unit,
    onHistoryEvent: (LocationHistoryUiEvent) -> Unit
) {
    var isMapLoaded by remember { mutableStateOf(false) }
    val homeMapController = rememberMapLibreHomeMapController(
        uiState = uiState,
        mapContentPadding = mapContentPadding,
        isMapLoaded = isMapLoaded,
        onMapTouch = {
            onMainEvent(MapHomeScreenUiEvent.OnMapTouch)
        }
    )

    val mapController = if (historyUiState != null) {
//        rememberLocationHistoryMapController(
//            uiState = historyUiState,
//            mapContentPadding = mapContentPadding,
//            cameraPositionState = cameraPositionState,
//            animateFirstContent = true,
//            isSatelliteMode = uiState.isSatelliteMode,
//            onMapTouch = {
//                onHistoryEvent(LocationHistoryUiEvent.OnMapTouch)
//            }
//        )
        homeMapController
    } else {
        homeMapController
    }

    GenericMapLibreMapWrapper(
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
                GoogleMapLocationHistoryMapContent(
                    uiState = historyUiState,
                    isSatelliteMode = uiState.isSatelliteMode,
                    onEvent = onHistoryEvent
                )
            } else {
                MapLibreMainHomeMapContent(
                    uiState = readyState,
                    onEvent = onMainEvent
                )
            }
        }
    )
}