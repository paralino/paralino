package app.paralino.android.ui.map

import android.view.MotionEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.history.MapUiControlledData
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.create.CreatePlaceScreenState
import app.paralino.core.presentation.places.create.PlaceMapFallbackLatLng
import app.paralino.core.presentation.places.detail.PlaceDetailScreenState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
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
        fallbackCenterLatLng = state.fallbackCenterLatLng,
        isSatelliteMode = state.isSatelliteMode,
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
        fallbackCenterLatLng = null,
        isSatelliteMode = false,
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
    fallbackCenterLatLng: PlaceMapFallbackLatLng?,
    canTapOnCircles: Boolean,
    isSatelliteMode: Boolean,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapInteraction: (Boolean) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {

    val scope = rememberCoroutineScope()
    var mapLoaded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    val mapController = rememberPlaceMapController(
        mapContentPadding = remember { PaddingValues() },
        cameraPositionState = cameraPositionState,
        isSatelliteMode = isSatelliteMode
    )

    if (mapLoaded) {
        LaunchedEffect(Unit) {
            if (markers.isEmpty() && fallbackCenterLatLng != null) {
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(
                        com.google.android.gms.maps.model.LatLng(
                            fallbackCenterLatLng.location.lat,
                            fallbackCenterLatLng.location.lng
                        ),
                        if (fallbackCenterLatLng.isRegion) 4f else 10f
                    )
                )
            } else {
                cameraPositionState.refocus(
                    scope = scope,
                    markers = markers,
                    padding = 150,
                    durationMs = 0
                )
            }
        }
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
                } catch (e: Exception) {
                }
                onMapInteraction(it)
            }
    }

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
        GenericGoogleMapWrapper(
            modifier = Modifier.fillMaxSize(),
            mapData = mapController,
            onMapLongClick = {
                onMapLongPress(it.lat, it.lng)
            },
            onMapClick = {
                onMapTap(it.lat, it.lng)
                onMapInteraction(true)
            },
            onMapLoaded = { mapLoaded = true },
            placeholder = {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.onBackground
                )
            },
            content = {
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
        )
    }
}

@Composable
private fun rememberPlaceMapController(
    mapContentPadding: PaddingValues,
    cameraPositionState: CameraPositionState,
    isSatelliteMode: Boolean
): MapUiControlledData {
    val uiSettings by defaultMapUiSettings()
    val properties by defaultMapProperties(
        satelliteMode = isSatelliteMode,
        isDarkMode = isSystemInDarkTheme()
    )

    return remember(mapContentPadding, isSatelliteMode) {
        derivedStateOf {
            MapUiControlledData(
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
                contentPadding = mapContentPadding
            )
        }
    }.value
}
