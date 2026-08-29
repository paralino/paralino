package app.paralino.android.ui.map.maplibre

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.asReady
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.RenderOptions

@Composable
fun rememberMapLibreHomeMapController(
    cameraState: CameraState = rememberCameraState(),
    uiState: MapHomeScreenState,
    mapContentPadding: PaddingValues,
    isMapLoaded: Boolean,
    onMapTouch: () -> Unit
): MapLibreMapUiControlledData {

    val stateData by rememberUpdatedState(newValue = uiState.asReady())
    val scope = rememberCoroutineScope()
    var canAnimateMapFocus by remember { mutableStateOf(false) }
    var shouldAnimateMapFocus by rememberSaveable { mutableStateOf(false) }
    val areEffectsEnabled = remember(stateData?.mapHomeScreenFocusType) { stateData?.locationHistoryMode == null }

    if (isMapLoaded && areEffectsEnabled) {
        LaunchedEffect(Unit) {
            delay(500)
            canAnimateMapFocus = true
            shouldAnimateMapFocus = true
        }

        LaunchedEffect(stateData?.visibleMarkers) {
            snapshotFlow { stateData?.mapHomeScreenFocusType }
                .distinctUntilChanged()
                .collect {
                    if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                        delay(20)
                    }
                    cameraState.focusOnMap(
                        scope = scope,
                        focusType = it,
                        markers = stateData?.visibleMarkers ?: emptyList(),
                        focusedMarker = stateData?.trackingMarker,
                        durationMs = if (canAnimateMapFocus) 6000 else 0,
                        mapContentPadding = mapContentPadding
                    )
                }
        }

        LaunchedEffect(stateData?.trackingMarker, stateData?.mapHomeScreenFocusType) {
            snapshotFlow { stateData?.mapHomeScreenFocusType }
                .distinctUntilChanged()
                .collectLatest {
                    if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                        delay(20)
                    }
                    cameraState.focusOnMap(
                        scope = scope,
                        focusType = it,
                        markers = stateData?.visibleMarkers ?: emptyList(),
                        focusedMarker = stateData?.trackingMarker,
                        durationMs = if (canAnimateMapFocus) 500 else 0,
                        mapContentPadding = mapContentPadding
                    )
                }
        }

        LaunchedEffect(key1 = cameraState) {
            snapshotFlow { cameraState.isCameraMoving }
                .distinctUntilChanged()
                .filter { it && cameraState.moveReason == CameraMoveReason.GESTURE }
                .collectLatest { onMapTouch() }
        }
    }

    return remember(mapContentPadding, uiState.isSatelliteModeActive, uiState.androidMapSDKProvider) {
        derivedStateOf {
            val options = MapOptions(
                renderOptions = RenderOptions(),
                gestureOptions = GestureOptions()
            )

            MapLibreMapUiControlledData(
                options = options,
                isSatelliteMode = uiState.isSatelliteModeActive,
                cameraState = cameraState,
                contentPadding = mapContentPadding,
                mapsSDKProvider = uiState.androidMapSDKProvider
            )
        }
    }.value
}