package app.paralino.android.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.logg
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.map.asFocusedMarker
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
        state = (uiState as? MapHomeScreenState.Ready),
        mapContentPadding = mapContentPadding,
        onMapLongPress = { lat, lng ->
            val groupId = (uiState as? MapHomeScreenState.Ready)?.groupId
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

    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        boundsForMarkers(state?.markers ?: emptyList())?.let {
            move(CameraUpdateFactory.newLatLngBounds(it, 40))
        }
    }

    val stateData by rememberUpdatedState(newValue = state)

    LaunchedEffect(stateData?.markers) {
        snapshotFlow { stateData?.mapHomeScreenFocusType }
            .distinctUntilChanged()
            .collectLatest {
                cameraPositionState.focusOnMap(
                    scope = scope,
                    focusType = it,
                    markers = stateData?.markers ?: emptyList(),
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
                    markers = stateData?.markers ?: emptyList(),
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
    val properties by defaultMapProperties(isDarkMode = isSystemInDarkTheme(), satelliteMode = satelliteMode)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = mapContentPadding,
        onMapLongClick = { onMapLongPress(it.latitude, it.longitude) },
        onMapClick = { onMapTouch() }
    ) {
        state?.markers?.forEach { model ->
            val latAnim = animateFloatAsState(
                targetValue = model.lat.toFloat(),
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
                label = "latAnim"
            ).value.toDouble()
            val lngAnim = animateFloatAsState(
                targetValue = model.lng.toFloat(),
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
                label = "lngAnim"
            ).value.toDouble()
            val accuracyAnim = animateFloatAsState(
                targetValue = model.accuracy,
                animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
                label = "accuracyAnim"
            ).value.toDouble()

            if (model.shouldShowApproximateCircle) {
                Circle(
                    center = LatLng(latAnim, lngAnim),
                    clickable = true,
                    fillColor = Color.Blue.copy(alpha = 0.4f),
                    radius = accuracyAnim,
                    strokeColor = Color.Blue,
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

            MapMarkerComposable(state = model, position = LatLng(latAnim, lngAnim), onClick = onMarkerClick)
        }
    }
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
