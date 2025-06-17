package app.paralino.android.ui.history

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.MapMarkerComposable
import app.paralino.android.ui.map.defaultMapProperties
import app.paralino.android.ui.map.defaultMapUiSettings
import app.paralino.android.ui.map.refocus
import app.paralino.core.presentation.history.LocationHistoryMapFocusType
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.theme.AppColors
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun LocationHistoryMapContent(
    uiState: LocationHistoryScreenState,
    mapContentPadding: PaddingValues,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {

    val stateData by rememberUpdatedState(newValue = uiState)
    val scope = rememberCoroutineScope()
    var canAnimateMapFocus by remember { mutableStateOf(false) }
    var shouldAnimateMapFocus by rememberSaveable { mutableStateOf(false) }
    val polylineWidthPixels = with(LocalDensity.current) { 4.dp.toPx() }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        delay(500)
        canAnimateMapFocus = true
        shouldAnimateMapFocus = true
    }

    LaunchedEffect(stateData.selectedLocations) {
        snapshotFlow { stateData.mapFocusType }
            .distinctUntilChanged()
            .collectLatest {
                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                    delay(20)
                }
                cameraPositionState.refocus(
                    scope = scope,
                    markers = stateData.selectedLocations.mapNotNull {
                        it.point.loc?.let { location ->
                            MapPinData(
                                groupId = stateData.groupId,
                                location = location,
                                name = null,
                                image = null,
                                timeInfo = null
                            )
                        }
                    },
                    ignoreAccuracy = it != LocationHistoryMapFocusType.LocationPoint,
                    durationMs = if (canAnimateMapFocus) 1000 else 0
                )
            }
    }

    LaunchedEffect(uiState.mapFocusType) {
        snapshotFlow { stateData.mapFocusType }
            .distinctUntilChanged()
            .collectLatest {
                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                    delay(20)
                }
                cameraPositionState.refocus(
                    scope = scope,
                    markers = stateData.selectedLocations.mapNotNull {
                        it.point.loc?.let { location ->
                            MapPinData(
                                groupId = stateData.groupId,
                                location = location,
                                name = null,
                                image = null,
                                timeInfo = null
                            )
                        }
                    },
                    ignoreAccuracy = it != LocationHistoryMapFocusType.LocationPoint,
                    durationMs = if (canAnimateMapFocus) 500 else 0
                )
            }
    }

    LaunchedEffect(uiState.selectedMapPinData) {
        snapshotFlow { stateData.selectedMapPinData }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest {
                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                    delay(20)
                }
                cameraPositionState.refocus(
                    scope = scope,
                    markers = listOf(it),
                    ignoreAccuracy = false,
                    durationMs = if (canAnimateMapFocus) 100 else 0,
                    forceZoom = false
                )
            }
    }

    LaunchedEffect(key1 = cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { it && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE }
            .collectLatest { onEvent(LocationHistoryUiEvent.OnMapTouch) }
    }

    val uiSettings by defaultMapUiSettings()
    val properties by defaultMapProperties(
        isDarkMode = isSystemInDarkTheme(),
        satelliteMode = uiState.isSatelliteMode
    )

    val points = remember(uiState.selectedLocations) {
        uiState.selectedLocations.mapNotNull {
            it.point.loc?.let {
                LatLng(it.lat, it.lng)
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = mapContentPadding,
        onMapLongClick = {
            onEvent(
                LocationHistoryUiEvent.OnMapLongPress(
                    lat = it.latitude,
                    lng = it.longitude
                )
            )
        },
        onMapClick = { onEvent(LocationHistoryUiEvent.OnMapTouch) }
    ) {
//        stateData.locations.mapNotNull { it.point.loc }?.forEach { model ->
//            Circle(
//                center = LatLng(model.lat, model.lng),
//                clickable = true,
//                fillColor = AppColors.accentDark.copy(alpha = 0.5f),
//                radius = model.acc.toDouble(),
//                strokeColor = Color.Blue,
//                strokeWidth = 4f,
//                zIndex = 10f
//            )
//        }

        when (uiState.mapFocusType) {
            LocationHistoryMapFocusType.LocationPoint -> {
                uiState.selectedMapPinData?.let { point ->

                    val latAnim = animateFloatAsState(
                        targetValue = point.lat.toFloat(),
                        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                        label = "latAnim"
                    ).value.toDouble()
                    val lngAnim = animateFloatAsState(
                        targetValue = point.lng.toFloat(),
                        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                        label = "lngAnim"
                    ).value.toDouble()
                    val accuracyAnim = animateFloatAsState(
                        targetValue = point.accuracy,
                        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                        label = "accuracyAnim"
                    ).value.toDouble()

                    Circle(
                        center = LatLng(latAnim, lngAnim),
                        clickable = true,
                        fillColor = AppColors.accentDark.copy(alpha = 0.5f),
                        radius = accuracyAnim,
                        strokeColor = Color.Blue,
                        strokeWidth = 4f,
                        zIndex = 10f
                    )

                    MapMarkerComposable(
                        state = point,
                        position = LatLng(latAnim, lngAnim),
                        zIndex = 20f,
                        onClick = {
                            onEvent(LocationHistoryUiEvent.OnMarkerClick(it))
                        }
                    )
                }
            }

            LocationHistoryMapFocusType.Stats -> {}
            LocationHistoryMapFocusType.Timeline -> {}
        }

        Polyline(
            points = points,
            clickable = true,
            color = MaterialTheme.colors.primary,
            startCap = RoundCap(),
            endCap = RoundCap(),
            geodesic = false,
            jointType = JointType.ROUND,
            tag = "points",
            visible = true,
            width = polylineWidthPixels,
            zIndex = 0f,
            onClick = { }
        )
    }
}
