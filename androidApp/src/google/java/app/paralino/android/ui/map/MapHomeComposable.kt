package app.paralino.android.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.logg
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.presentation.map.asReady
import app.paralino.core.ui.theme.AppColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun MainHomeMapComposable(
    appState: MainAppState,
    uiState: MapHomeScreenState,
    vm: MapHomeViewModel,
    mapContentPadding: PaddingValues = PaddingValues(),
) {
    MainHomeMapComposable(
        satelliteMode = uiState.isSatelliteMode,
        state = uiState.asReady(),
        mapContentPadding = mapContentPadding,
        onMapLongPress = { lat, lng ->
            val groupId = uiState.asReady()?.groupId
            if (groupId != null) {
                appState.navController.navigate(
                    AppScreen.MainHome.PlaceCreate.route(
                        groupId = groupId,
                        placeId = null,
                        lat = lat,
                        lng = lng
                    )
                )
            }
        },
        onMapTouch = {
            vm.onMapTouch()
        },
        onMarkerClick = {
            logg.tag("marker_click").v("Marker clicked: ${it.pinType}")
            vm.trackMarker(it.asFocusedMarker())
        }
    )
}

@Composable
fun MainHomeMapComposable(
    satelliteMode: Boolean,
    state: MapHomeScreenState.Ready?,
    mapContentPadding: PaddingValues,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
    onMapTouch: () -> Unit,
    onMarkerClick: (MapPinData) -> Unit
) {

    val stateData by rememberUpdatedState(newValue = state)

    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        boundsForMarkers(stateData?.visibleMarkers ?: emptyList())?.let {
            move(CameraUpdateFactory.newLatLngBounds(it, 40))
        }
    }

    LaunchedEffect(stateData?.visibleMarkers) {
        snapshotFlow { stateData?.mapHomeScreenFocusType }
            .distinctUntilChanged()
            .collectLatest {
                cameraPositionState.focusOnMap(
                    scope = scope,
                    focusType = it,
                    markers = stateData?.visibleMarkers ?: emptyList(),
                    focusedMarker = stateData?.trackingMarker,
                    durationMs = 3000
                )
            }
    }

    LaunchedEffect(state?.trackingMarker, state?.mapHomeScreenFocusType) {
        snapshotFlow { stateData?.mapHomeScreenFocusType }
            .distinctUntilChanged()
            .collectLatest {
                cameraPositionState.focusOnMap(
                    scope = scope,
                    focusType = it,
                    markers = stateData?.visibleMarkers ?: emptyList(),
                    focusedMarker = stateData?.trackingMarker,
                    durationMs = 500
                )
            }
    }

    LaunchedEffect(key1 = cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { it && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE }
            .collectLatest { onMapTouch() }
    }

    val uiSettings by defaultMapUiSettings()
    val properties by defaultMapProperties(
        isDarkMode = isSystemInDarkTheme(),
        satelliteMode = satelliteMode
    )

    val markerClick: (MapPinData) -> Unit = {
        onMarkerClick(it)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = mapContentPadding,
        onMapLongClick = { onMapLongPress(it.latitude, it.longitude) },
        onMapClick = { onMapTouch() }
    ) {
        stateData?.visibleMarkers?.forEach { model ->
            MapMarkerComposable(
                model = model,
                satelliteMode = satelliteMode,
                onMarkerClick = markerClick
            )
        }
    }
}

@Composable
private fun MapMarkerComposable(
    model: MapPinData,
    satelliteMode: Boolean,
    onMarkerClick: (MapPinData) -> Unit
) {

    val lat = remember { Animatable(0f) }
    val lng = remember { Animatable(0f) }
    val acc = remember { Animatable(0f) }
    val animationSpec = remember {
        tween<Float>(durationMillis = 3000, easing = LinearEasing)
    }

    LaunchedEffect(model.markerId) {
        lat.snapTo(model.lat.toFloat())
        lng.snapTo(model.lng.toFloat())
        acc.snapTo(model.accuracy)
    }

    LaunchedEffect(model.lat) {
        lat.animateTo(model.lat.toFloat(), animationSpec = animationSpec)
    }

    LaunchedEffect(model.lng) {
        lng.animateTo(model.lng.toFloat(), animationSpec = animationSpec)
    }

    LaunchedEffect(model.accuracy) {
        acc.animateTo(model.accuracy.toFloat(), animationSpec = animationSpec)
    }

    if (model.shouldShowApproximateCircle) {
        Circle(
            center = LatLng(lat.value.toDouble(), lng.value.toDouble()),
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
            onClick = {
                onMarkerClick(model)
            }
        )
    }

    MapMarkerComposable(
        state = model,
        position = LatLng(lat.value.toDouble(), lng.value.toDouble()),
        onClick = onMarkerClick
    )
}

private fun CameraPositionState.focusOnMap(
    scope: CoroutineScope,
    focusType: MapHomeScreenFocusType?,
    markers: List<MapPinData>,
    focusedMarker: FocusedMarkerData?,
    durationMs: Int
) {
    val focus = when (focusType) {
        is MapHomeScreenFocusType.PLACE,
        is MapHomeScreenFocusType.MEMBER -> {
            listOfNotNull(
                markers.firstOrNull { it.markerId == focusedMarker?.markerId }
            )
        }

        MapHomeScreenFocusType.ALL_PLACES -> {
            markers.filter { it.pinType == MapPinType.PLACE }
        }

        MapHomeScreenFocusType.ALL_MEMBERS -> {
            markers.filter { it.pinType == MapPinType.MEMBER }
        }

        else -> {
            emptyList()
        }
    }

    val padding = when (focusType) {
        MapHomeScreenFocusType.ALL_MEMBERS -> {
            if (markers.size == 1) 200 else 100
        }

        MapHomeScreenFocusType.ALL_PLACES -> {
            if (markers.size == 1) 300 else 100
        }

        else -> 100
    }

    val forceZoom = when (focusType) {
        is MapHomeScreenFocusType.MEMBER -> focusType.forceZoom
        else -> true
    }
    refocus(
        scope = scope,
        markers = focus,
        forceZoom = forceZoom,
        padding = 100,
        durationMs = durationMs
    )
}
