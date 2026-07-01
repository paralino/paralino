package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.paralino.android.ui.map.maplibre.history.MapLibreLocationHistoryMapContent
import app.paralino.android.ui.map.maplibre.history.rememberMapLibreLocationHistoryMapController
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.models.location.LatLng
import org.maplibre.compose.camera.rememberCameraState


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
    val cameraState = rememberCameraState()

    val homeMapController = rememberMapLibreHomeMapController(
        uiState = uiState,
        cameraState = cameraState,
        mapContentPadding = mapContentPadding,
        isMapLoaded = isMapLoaded,
        onMapTouch = {
            onMainEvent(MapHomeScreenUiEvent.OnMapTouch)
        }
    )

    val mapController = if (historyUiState != null) {
        rememberMapLibreLocationHistoryMapController(
            uiState = historyUiState,
            cameraState = cameraState,
            mapContentPadding = mapContentPadding,
            isMapLoaded = isMapLoaded,
            animateFirstContent = true,
            isSatelliteMode = uiState.isSatelliteModeActive,
            mapsSDKProvider = uiState.androidMapSDKProvider,
            onMapTouch = {
                onHistoryEvent(LocationHistoryUiEvent.OnMapTouch)
            }
        )
    } else {
        homeMapController
    }

    Box(modifier = modifier) {
        GenericMapLibreMapWrapper(
            modifier = Modifier.fillMaxSize(),
            mapData = mapController,
            isSystemInDarkTheme = isSystemInDarkTheme(),
            onMapClick = {
                onMainEvent(MapHomeScreenUiEvent.OnMapClick(it))
            },
            onMapLongClick = {
                onMainEvent(MapHomeScreenUiEvent.OnMapLongPress(it))
            },
            onMapLoaded = {
                isMapLoaded = true
            },
            content = {
                if (historyUiState != null) {
                    MapLibreLocationHistoryMapContent(
                        uiState = historyUiState,
                        isSatelliteMode = uiState.isSatelliteModeActive,
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

        when {
            historyUiState?.tappedCoordinateState != null -> {
                val tappedState = historyUiState.tappedCoordinateState!!
                MapLibreTapActionOverlay(
                    modifier = Modifier.fillMaxSize(),
                    tappedState = tappedState,
                    cameraState = cameraState,
                    showPrivacyZoneButton = !historyUiState.isPersonalHistory,
                    onCreatePlace = {
                        onHistoryEvent(
                            LocationHistoryUiEvent.OnOpenCreatePlaceScreen(
                                location = LatLng(lat = tappedState.lat, lng = tappedState.lng)
                            )
                        )
                    },
                    onCreatePrivacyZone = {
                        onHistoryEvent(
                            LocationHistoryUiEvent.OnOpenCreatePrivacyZoneScreen(
                                location = LatLng(lat = tappedState.lat, lng = tappedState.lng)
                            )
                        )
                    }
                )
            }

            readyState?.tappedCoordinateState != null -> {
                val tappedState = readyState.tappedCoordinateState!!
                MapLibreTapActionOverlay(
                    modifier = Modifier.fillMaxSize(),
                    tappedState = tappedState,
                    cameraState = cameraState,
                    showPrivacyZoneButton = !readyState.isPersonalSpace,
                    onCreatePlace = {
                        onMainEvent(
                            MapHomeScreenUiEvent.OnOpenCreatePlaceScreen(
                                location = LatLng(lat = tappedState.lat, lng = tappedState.lng)
                            )
                        )
                    },
                    onCreatePrivacyZone = {
                        onMainEvent(
                            MapHomeScreenUiEvent.OnOpenCreatePrivacyZoneScreen(
                                location = LatLng(lat = tappedState.lat, lng = tappedState.lng)
                            )
                        )
                    }
                )
            }
        }
    }
}