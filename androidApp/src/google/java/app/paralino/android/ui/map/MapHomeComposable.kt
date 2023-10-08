package app.paralino.android.ui.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun MainHomeMapComposable(
    isDarkMode: Boolean,
    satelliteMode: Boolean,
    state: MapHomeScreenState.Ready?,
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

    LaunchedEffect(key1 = stateData?.mapHomeScreenFocusType, key2 = stateData?.markers) {
        snapshotFlow { stateData?.mapHomeScreenFocusType }
            .distinctUntilChanged()
            .collectLatest {
                val focus = when (it) {
                    MapHomeScreenFocusType.PLACE,
                    MapHomeScreenFocusType.MEMBER -> {
                        listOfNotNull(
                            stateData?.markers?.firstOrNull { it.markerId == stateData?.focusedMarker?.markerId }
                        )
                    }

                    MapHomeScreenFocusType.ALL_PLACES -> {
                        stateData?.markers?.filter { it.pinType == MapPinType.PLACE } ?: emptyList()
                    }

                    MapHomeScreenFocusType.ALL_MEMBERS -> {
                        stateData?.markers?.filter { it.pinType == MapPinType.MEMBER } ?: emptyList()
                    }

                    else -> {
                        emptyList()
                    }
                }
                cameraPositionState.refocus(
                    scope = scope,
                    markers = focus,
                    padding = 100,
                    durationMs = 3000
                )
            }
    }

    LaunchedEffect(key1 = cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { it && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE }
            .collectLatest { onMapTouch() }
    }

    var uiSettings by defaultMapUiSettings()
    var properties by defaultMapProperties(isDarkMode = isDarkMode, satelliteMode = satelliteMode)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
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
                    strokeWidth = 8f,
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
