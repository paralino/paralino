package app.paralino.android.ui.history

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.GenericMapWrapper
import app.paralino.android.ui.map.MapMarkerComposable
import app.paralino.android.ui.map.defaultMapProperties
import app.paralino.android.ui.map.defaultMapUiSettings
import app.paralino.android.ui.map.refocus
import app.paralino.core.presentation.history.LocationHistoryDataViewType
import app.paralino.core.presentation.history.LocationHistoryPointState
import app.paralino.core.presentation.history.LocationHistoryScreenFocusType
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.isLocationInaccurate
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun LocationHistoryMapWithContent(
    uiState: LocationHistoryScreenUiState,
    mapContentPadding: PaddingValues,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState()
    val mapController = rememberLocationHistoryMapController(
        uiState = uiState,
        mapContentPadding = mapContentPadding,
        cameraPositionState = cameraPositionState,
        animateFirstContent = false,
        isSatelliteMode = uiState.isSatelliteMode,
        onMapTouch = { onEvent(LocationHistoryUiEvent.OnMapTouch) }
    )

    GenericMapWrapper(
        modifier = Modifier.fillMaxSize(),
        mapData = mapController,
        onMapLoaded = {},
        onMapLongClick = {
            onEvent(
                LocationHistoryUiEvent.OnMapLongPress(
                    lat = it.lat,
                    lng = it.lng
                )
            )
        },
        onMapClick = { onEvent(LocationHistoryUiEvent.OnMapTouch) }
    ) {
        LocationHistoryMapContent(
            uiState = uiState,
            onEvent = onEvent
        )
    }
}

@Composable
fun rememberLocationHistoryMapController(
    uiState: LocationHistoryScreenUiState,
    mapContentPadding: PaddingValues,
    cameraPositionState: CameraPositionState,
    isSatelliteMode: Boolean,
    animateFirstContent: Boolean,
    onMapTouch: () -> Unit
): MapUiControlledData {
    val stateData by rememberUpdatedState(newValue = uiState)
    val scope = rememberCoroutineScope()
    var canAnimateMapFocus by remember { mutableStateOf(animateFirstContent) }
    var shouldAnimateMapFocus by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        canAnimateMapFocus = true
        shouldAnimateMapFocus = true
    }

    LaunchedEffect(stateData.selectedLocations, stateData.dataViewType, mapContentPadding) {
        snapshotFlow { stateData.focusType }
            .distinctUntilChanged()
            .filter { it == LocationHistoryScreenFocusType.OVERVIEW }
            .collectLatest {
//                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
//                    delay(20)
//                }
                cameraPositionState.refocus(
                    scope = scope,
                    markers = stateData.selectedLocations.mapNotNull {
                        when (it) {
                            is LocationHistoryPointState.Cluster -> {
                                MapPinData(
                                    id = "",
                                    groupId = stateData.groupOrSpaceId,
                                    cluster = it,
                                    name = null,
                                    image = null,
                                    timeInfo = null
                                )
                            }
                            is LocationHistoryPointState.Point -> {
                                it.point.loc?.let { location ->
                                    MapPinData(
                                        id = it.point.properID,
                                        groupId = stateData.groupOrSpaceId,
                                        location = location,
                                        name = null,
                                        image = null,
                                        timeInfo = null
                                    )
                                }
                            }
                        }
                    },
                    forceZoom = true,
                    ignoreAccuracy = false,
                    durationMs = if (canAnimateMapFocus) 600 else 0,
                    padding = 200
                )
            }
    }

    LaunchedEffect(uiState.selectedMapPinData, uiState.focusType) {
        snapshotFlow { stateData.focusType }
            .filterNotNull()
            .distinctUntilChanged()
            .filter { it.isMember }
            .collectLatest {
                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                    delay(20)
                }
                cameraPositionState.refocus(
                    scope = scope,
                    markers = listOfNotNull(uiState.selectedMapPinData),
                    ignoreAccuracy = false,
                    durationMs = if (canAnimateMapFocus) 300 else 0,
                    forceZoom = it == LocationHistoryScreenFocusType.MEMBER_FOCUSED,
                    padding = 60
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
        satelliteMode = isSatelliteMode
    )

    return remember {
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

@Composable
fun LocationHistoryMapContent(
    uiState: LocationHistoryScreenUiState,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    val polylineWidthPixels = with(LocalDensity.current) { 5.dp.toPx() }

    when (uiState.dataViewType) {
        LocationHistoryDataViewType.LocationPoint -> {
            uiState.selectedMapPinData?.let { point ->

                val latAnim = animateFloatAsState(
                    targetValue = point.lat.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "latAnim"
                ).value.toDouble()
                val lngAnim = animateFloatAsState(
                    targetValue = point.lng.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "lngAnim"
                ).value.toDouble()
                val accuracyAnim = animateFloatAsState(
                    targetValue = point.accuracy,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "accuracyAnim"
                ).value.toDouble()

                if (point.shouldShowApproximateCircle) {
                    Circle(
                        center = com.google.android.gms.maps.model.LatLng(latAnim, lngAnim),
                        clickable = true,
                        fillColor = AppColors.accentDark.copy(alpha = 0.5f),
                        radius = accuracyAnim,
                        strokeColor = Color.Blue,
                        strokeWidth = 4f,
                        zIndex = 10f
                    )
                }

                MapMarkerComposable(
                    state = point,
                    position = com.google.android.gms.maps.model.LatLng(latAnim, lngAnim),
                    zIndex = 20f,
                    onClick = {
                        onEvent(LocationHistoryUiEvent.OnMarkerClick(it))
                    }
                )
            }
        }

        LocationHistoryDataViewType.Stats -> {}
    }

    if (uiState.showPolyline) {
        val points = remember(uiState.selectedLocations) {
            uiState.selectedLocations.mapNotNull {
                when (it) {
                    is LocationHistoryPointState.Cluster -> {
                        com.google.android.gms.maps.model.LatLng(it.lat, it.lng)
                    }
                    is LocationHistoryPointState.Point -> {
                        it.point.loc?.let {
                            com.google.android.gms.maps.model.LatLng(it.lat, it.lng)
                        }
                    }
                }
            }
        }

        Polyline(
            points = points,
            clickable = true,
            color = if (uiState.isSatelliteMode || isSystemInDarkTheme()) {
                AppColors.redMain
            } else {
                MaterialTheme.colors.primary
            },
            pattern = null,
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

    if (uiState.showPins) {
        if (uiState.clusterPins) {
            val clusterPoints = remember(uiState.selectedLocations) {
                uiState.selectedLocations.mapNotNull {
                    if (it.lat != null && it.lng != null) {
                        LocationHistoryClusterItem(it)
                    } else {
                        null
                    }
                }
            }

            Clustering<LocationHistoryClusterItem>(
                items = clusterPoints,
                onClusterClick = {
                    try {
                        onEvent(LocationHistoryUiEvent.OnPointClicked(it.items.first().item))
                    } catch (e: Exception) { }
                    false
                },
                onClusterItemClick = {
                    onEvent(LocationHistoryUiEvent.OnPointClicked(it.item))
                    true
                },
                clusterContent = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.highlightGold)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.size.toString(),
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.darkBackground
                        )
                    }
                },
                clusterItemContent = {
                    Icon(
                        modifier = Modifier
                            .size(
                                if (it.item.accuracy.isLocationInaccurate()) {
                                    18.dp
                                } else {
                                    16.dp
                                }
                            ),
                        painter = if (it.item.accuracy.isLocationInaccurate()) {
                            CoreAppIcons.NotListedLocationFilled.asPainterResource()
                        } else {
                            CoreAppIcons.LocationOnFilled.asPainterResource()
                        },
                        contentDescription = null,
                        tint = if (uiState.isSatelliteMode || isSystemInDarkTheme()) {
                            AppColors.highlightGold
                        } else {
                            AppColors.redMain
                        }
                    )
                },
            )
        } else {
            uiState.selectedLocations.forEach { pin ->
                if (pin.lat != null && pin.lng != null) {
                    HistoryMapPin(
                        state = pin,
                        position = com.google.android.gms.maps.model.LatLng(pin.lat!!, pin.lng!!),
                        isDarkMode = uiState.isSatelliteMode || isSystemInDarkTheme(),
                        zIndex = 1f,
                        onClick = {
                            onEvent(LocationHistoryUiEvent.OnPointClicked(it))
                        }
                    )
                }
            }
        }
    }
}

@Stable
class MapUiControlledData(
    val cameraPositionState: CameraPositionState,
    val properties: MapProperties,
    val uiSettings: MapUiSettings,
    val contentPadding: PaddingValues
)