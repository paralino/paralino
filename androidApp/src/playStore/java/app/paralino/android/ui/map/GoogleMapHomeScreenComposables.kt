package app.paralino.android.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.theme.AppColors
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun rememberGoogleMapMainHomeMapController(
    uiState: MapHomeScreenState,
    mapContentPadding: PaddingValues,
    cameraPositionState: CameraPositionState,
    isMapLoaded: Boolean,
    onMapTouch: () -> Unit
): GoogleMapUiControlledData {

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
                .collectLatest {
                    if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                        delay(20)
                    }
                    cameraPositionState.focusOnMap(
                        scope = scope,
                        focusType = it,
                        markers = stateData?.visibleMarkers ?: emptyList(),
                        focusedMarker = stateData?.trackingMarker,
                        durationMs = if (canAnimateMapFocus) 3000 else 0
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
                    cameraPositionState.focusOnMap(
                        scope = scope,
                        focusType = it,
                        markers = stateData?.visibleMarkers ?: emptyList(),
                        focusedMarker = stateData?.trackingMarker,
                        durationMs = if (canAnimateMapFocus) 500 else 0
                    )
                }
        }

        LaunchedEffect(key1 = cameraPositionState) {
            snapshotFlow { cameraPositionState.isMoving }
                .distinctUntilChanged()
                .filter { it && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE }
                .collectLatest { onMapTouch() }
        }
    }

    val uiSettings by defaultGoogleMapUiSettings()
    val properties by defaultGoogleMapProperties(
        isDarkMode = isSystemInDarkTheme(),
        satelliteMode = uiState.isSatelliteMode
    )

    return remember(mapContentPadding, uiState.isSatelliteMode) {
        derivedStateOf {
            GoogleMapUiControlledData(
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                contentPadding = mapContentPadding
            )
        }
    }.value
}

@Composable
fun GoogleMapMainHomeMapContent(
    uiState: MapHomeScreenState.Ready?,
    onEvent: (MapHomeScreenUiEvent) -> Unit
) {
    uiState?.visibleMarkers?.forEach { model ->
        MapHomeMarkerComposable(
            model = model,
            satelliteMode = uiState.isSatelliteMode,
            onMarkerClick = { onEvent(MapHomeScreenUiEvent.OnMarkerClick(it)) }
        )
    }
}


@Composable
private fun MapHomeMarkerComposable(
    model: MapPinData,
    satelliteMode: Boolean,
    onMarkerClick: (MapPinData) -> Unit
) {

    val lat = remember { Animatable(0f) }
    val lng = remember { Animatable(0f) }
    val acc = remember { Animatable(0f) }

    var latValue by remember { mutableDoubleStateOf(0.0) }
    var lngValue by remember { mutableDoubleStateOf(0.0) }

    val animationSpec = remember {
        tween<Float>(durationMillis = 4000, easing = LinearEasing)
    }

    val zIndex = when (model.pinType) {
        MapPinType.MEMBER -> if (model.focused) 40f else 30f
        MapPinType.PLACE -> if (model.focused) 40f else 20f
    }

    LaunchedEffect(model.markerId) {
        lat.snapTo(0f)
        lng.snapTo(0f)
        acc.snapTo(model.accuracy)

        latValue = model.lat
        lngValue = model.lng
    }

    LaunchedEffect(model.lat) {
        lat.snapTo(0f)
        val startLat = latValue
        lat.animateTo(1f, animationSpec = animationSpec) {
            latValue = (model.lat - startLat) * this.value + startLat
        }
    }

    LaunchedEffect(model.lng) {
        lng.snapTo(0f)
        val startLng = lngValue
        lng.animateTo(1f, animationSpec = animationSpec) {
            lngValue = (model.lng - startLng) * this.value + startLng
        }
    }

    LaunchedEffect(model.accuracy) {
        acc.animateTo(model.accuracy, animationSpec = animationSpec)
    }

    if (model.shouldShowApproximateCircle) {
        Circle(
            center = LatLng(latValue, lngValue),
            clickable = true,
            fillColor = when (model.pinType) {
                MapPinType.MEMBER -> AppColors.accentDark.copy(alpha = 0.5f)
                MapPinType.PLACE -> {
                    if (satelliteMode) {
                        Color.Blue.copy(alpha = 0.2f)
                    } else {
                        if (isSystemInDarkTheme()) {
                            AppColors.accent.copy(alpha = 0.15f)
                        } else {
                            Color.Blue.copy(alpha = 0.15f)
                        }
                    }
                }
            },
            radius = acc.value.toDouble(),
            strokeColor = when (model.pinType) {
                MapPinType.MEMBER -> Color.White
                MapPinType.PLACE -> Color.Blue
            },
            strokeWidth = when (model.pinType) {
                MapPinType.MEMBER -> 8f
                MapPinType.PLACE -> 0f
            },
            tag = model.markerId,
            zIndex = if (model.focused) 11f else 10f,
            onClick = {
                onMarkerClick(model)
            }
        )
    }

    GoogleMapMarkerComposable(
        state = model,
        position = LatLng(latValue, lngValue),
        canTapOnMarker = true,
        zIndex = zIndex,
        onClick = onMarkerClick
    )
}
