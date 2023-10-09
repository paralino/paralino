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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.logg
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.map.asFocusedMarker
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
    appState: MainAppState,
    uiState: MapHomeScreenState,
    vm: MapHomeViewModel
) {
    MainHomeMapComposable(
        satelliteMode = uiState.isSatelliteMode,
        state = (uiState as? MapHomeScreenState.Ready),
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
            when (it.pinType) {
                MapPinType.MEMBER -> {
                    logg.tag("marker_click").v("Member marker clicked")
                    vm.trackMarker(it.asFocusedMarker())
                }

                MapPinType.PLACE -> {
                    if (it.focused) {
                        appState.navController.navigate(
                            AppScreen.MainHome.PlaceDetail.route(
                                groupId = it.groupId,
                                placeId = it.markerId
                            )
                        )
                    } else {
                        vm.trackMarker(it.asFocusedMarker())
                    }
                }
            }
        }
    )
}

@Composable
fun MainHomeMapComposable(
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

    LaunchedEffect(stateData?.markers, state?.trackingMarker) {
        snapshotFlow { stateData?.mapHomeScreenFocusType }
            .distinctUntilChanged()
            .collectLatest {
                val focus = when (it) {
                    is MapHomeScreenFocusType.PLACE,
                    is MapHomeScreenFocusType.MEMBER -> {
                        listOfNotNull(
                            stateData?.markers?.firstOrNull { it.markerId == stateData?.trackingMarker?.markerId }
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
    var properties by defaultMapProperties(isDarkMode = isSystemInDarkTheme(), satelliteMode = satelliteMode)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = PaddingValues(bottom = if(state?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true) 0.dp else 140.dp),
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
