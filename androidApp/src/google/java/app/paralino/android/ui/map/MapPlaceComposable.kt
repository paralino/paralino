package app.paralino.android.ui.map

import android.view.MotionEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged


@Composable
fun PlaceCreateMapComposable(
    modifier: Modifier = Modifier,
    state: CreatePlaceScreenState,
    onMapInteraction: (Boolean) -> Unit,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {
    val pin = remember(state.lat, state.lng, state.radiusMeters, state.image) {
        if (state.lat != null && state.lng != null)
            MapPinData(state)
        else null
    }
    SimplePlaceMapComposable(
        modifier = modifier,
        markers = listOfNotNull(pin),
        canTapOnCircles = false,
        onMapCameraSettled = onMapCameraSettled,
        onMapInteraction = onMapInteraction,
        onMapTap = onMapTap,
        onMapLongPress = onMapLongPress
    )
}

@Composable
fun PlaceDetailMapComposable(
    modifier: Modifier = Modifier,
    state: PlaceDetailScreenState,
    onMapInteraction: (Boolean) -> Unit,
) {
    val pin = remember(state.lat, state.lng, state.radiusMeters, state.image) {
        if (state.lat != null && state.lng != null)
            MapPinData(state)
        else null
    }
    SimplePlaceMapComposable(
        modifier = modifier,
        markers = listOfNotNull(pin),
        canTapOnCircles = true,
        onMapCameraSettled = {},
        onMapInteraction = onMapInteraction,
        onMapTap = { lat, lng -> },
        onMapLongPress = { lat, lng -> }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimplePlaceMapComposable(
    modifier: Modifier = Modifier,
    markers: List<MapPinData>,
    canTapOnCircles: Boolean,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapInteraction: (Boolean) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {

    val scope = rememberCoroutineScope()
    var mapLoaded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        refocus(scope = scope, markers = markers, padding = 150, durationMs = 0)
    }

    LaunchedEffect(key1 = markers) {
        if (mapLoaded) {
            cameraPositionState.refocus(scope = scope, markers = markers)
        }
    }

    LaunchedEffect(key1 = cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .collectLatest {
                try {
                    onMapCameraSettled(
                        LatLng(
                            lat = cameraPositionState.position.target.latitude,
                            lng = cameraPositionState.position.target.longitude,
                        )
                    )
                } catch (e: Exception) { }
                onMapInteraction(it)
            }
    }

    val uiSettings by defaultMapUiSettings()
    val properties by defaultMapProperties(
        satelliteMode = false,
        isDarkMode = isSystemInDarkTheme()
    )

    Box(
        modifier = modifier.pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    onMapInteraction(true)
                }

                else -> {
                    onMapInteraction(false)
                }
            }
            false
        },
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings,
            onMapLongClick = { onMapLongPress(it.latitude, it.longitude) },
            onMapClick = {
                onMapTap(it.latitude, it.longitude)
                onMapInteraction(true)
            },
            onMapLoaded = { mapLoaded = true }
        ) {
            markers.forEach { model ->
                val latAnim = animateFloatAsState(
                    targetValue = model.lat.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "latAnim"
                ).value.toDouble()
                val lngAnim = animateFloatAsState(
                    targetValue = model.lng.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "lngAnim"
                ).value.toDouble()
                val accuracyAnim = animateFloatAsState(
                    targetValue = model.accuracy,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "accuracyAnim"
                ).value.toDouble()

                if (model.shouldShowApproximateCircle) {
                    Circle(
                        center = com.google.android.gms.maps.model.LatLng(latAnim, lngAnim),
                        clickable = canTapOnCircles,
                        fillColor = Color.Blue.copy(alpha = 0.4f),
                        radius = accuracyAnim,
                        strokeColor = Color.Blue,
                        strokeWidth = 0f,
                        tag = model.markerId,
                        zIndex = 10f,
                        onClick = {
                            cameraPositionState.refocus(scope = scope, markers = markers)
                        }
                    )
                }
                MapMarkerComposable(
                    state = model,
                    position = com.google.android.gms.maps.model.LatLng(latAnim, lngAnim),
                    canTapOnMarker = canTapOnCircles,
                    zIndex = 20f,
                    onClick = {
                        cameraPositionState.refocus(scope = scope, markers = markers)
                    }
                )
            }
        }
    }
}
