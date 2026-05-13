package app.paralino.android.ui.map.google.history

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.google.GoogleMapMarkerComposable
import app.paralino.android.ui.map.google.GoogleMapUiControlledData
import app.paralino.android.ui.map.google.defaultGoogleMapProperties
import app.paralino.android.ui.map.google.defaultGoogleMapUiSettings
import app.paralino.android.ui.map.google.refocus
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.presentation.history.LocationHistoryDataViewType
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.map.HistoryMapIcon
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.isLocationInaccurate
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.clustering.ClusteringMarkerProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull


@Composable
fun rememberGoogleMapLocationHistoryMapController(
    uiState: LocationHistoryScreenUiState,
    mapContentPadding: PaddingValues,
    cameraPositionState: CameraPositionState,
    isSatelliteMode: Boolean,
    animateFirstContent: Boolean,
    isMapLoaded: Boolean,
    onMapTouch: () -> Unit
): GoogleMapUiControlledData {
    val stateData by rememberUpdatedState(newValue = uiState)
    val scope = rememberCoroutineScope()
    var canAnimateMapFocus by remember { mutableStateOf(animateFirstContent) }
    var shouldAnimateMapFocus by rememberSaveable { mutableStateOf(false) }

    if (isMapLoaded) {
        LaunchedEffect(Unit) {
            delay(300)
            canAnimateMapFocus = true
            shouldAnimateMapFocus = true
        }

        LaunchedEffect(
            stateData.selectedLocations,
            stateData.dataViewType,
            stateData.canCollapseMapHeader,
            mapContentPadding
        ) {
            snapshotFlow { stateData.focusType }
                .distinctUntilChanged()
                .filter { it.isInOverview }
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
                        padding = if (stateData.canCollapseMapHeader) 40 else 200
                    )
                }
        }

        LaunchedEffect(
            uiState.selectedMapPinData,
            uiState.canCollapseMapHeader,
            uiState.focusType
        ) {
            snapshotFlow { stateData.focusType }
                .filterNotNull()
                .distinctUntilChanged()
                .filter { it.isMember }
                .collectLatest {
                    if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                        delay(20)
                    } else {
                        delay(200)
                    }

                    cameraPositionState.refocus(
                        scope = scope,
                        markers = listOfNotNull(uiState.selectedMapPinData),
                        ignoreAccuracy = false,
                        durationMs = if (canAnimateMapFocus) 300 else 0,
                        forceZoom = it.isMemberFocused,
                        padding = if (stateData.canCollapseMapHeader) 12 else 60
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
        satelliteMode = isSatelliteMode
    )

    return remember(mapContentPadding, isSatelliteMode) {
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
fun GoogleMapLocationHistoryMapContent(
    uiState: LocationHistoryScreenUiState,
    isSatelliteMode: Boolean,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    HistoryPolyline(
        showPolyline = uiState.showPolyline,
        selectedLocations = uiState.selectedLocations,
        isSatelliteMode = isSatelliteMode,
        onEvent = onEvent
    )

    HistoryPins(
        showAllPins = uiState.showPins,
        isPinClusteringOn = uiState.clusteringPinsMode.isClusteringOn,
        selectedLocations = uiState.selectedLocations,
        onEvent = onEvent
    )

    SelectedHistoryPointMarker(
        dataViewType = uiState.dataViewType,
        selectedMarker = uiState.selectedMapPinData,
        onEvent = onEvent
    )
}

@Composable
private fun HistoryPolyline(
    showPolyline: Boolean,
    selectedLocations: List<LocationHistoryPointState>,
    isSatelliteMode: Boolean,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    val polylineWidthPixels = with(LocalDensity.current) { 5.dp.toPx() }
    if (showPolyline) {
        val points = remember(selectedLocations) {
            selectedLocations.mapNotNull {
                when (it) {
                    is LocationHistoryPointState.Cluster -> {
                        LatLng(it.lat, it.lng)
                    }

                    is LocationHistoryPointState.Point -> {
                        it.point.loc?.let {
                            LatLng(it.lat, it.lng)
                        }
                    }
                }
            }
        }

        Polyline(
            points = points,
            clickable = true,
            color = if (isSatelliteMode || isSystemInDarkTheme()) {
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
            onClick = {
                onEvent(
                    LocationHistoryUiEvent.ChangeMapDataViewType(LocationHistoryDataViewType.LocationPoint)
                )
            }
        )
    }
}

@Composable
private fun HistoryStartStopPointPins(
    selectedLocations: List<LocationHistoryPointState>,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    val startStopLabeled = remember(selectedLocations) {
        val locations = selectedLocations
        when {
            locations.isEmpty() -> emptyList()
            locations.size == 1 -> listOf(locations.first() to "A")
            else -> listOf(
                locations.first() to "A",
                locations.last() to "B"
            )
        }
    }
    startStopLabeled.forEach { (state, label) ->
        key(state.pointId, label) {
            val lat = state.lat
            val lng = state.lng
            if (lat != null && lng != null) {
                GoogleMapHistoryStartStopMapPin(
                    pointId = state.pointId,
                    content = label,
                    position = LatLng(lat, lng),
                    zIndex = 1f,
                    onClick = { onEvent(LocationHistoryUiEvent.OnPointIdClicked(it)) }
                )
            }
        }
    }
}

@Composable
private fun SelectedHistoryPointMarker(
    dataViewType: LocationHistoryDataViewType,
    selectedMarker: MapPinData?,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    when (dataViewType) {
        LocationHistoryDataViewType.LocationPoint -> {
            selectedMarker?.let { point ->

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
                        center = LatLng(latAnim, lngAnim),
                        clickable = true,
                        fillColor = AppColors.accentDark.copy(alpha = 0.5f),
                        radius = accuracyAnim,
                        strokeColor = Color.Blue,
                        strokeWidth = 4f,
                        zIndex = 10f
                    )
                }

                GoogleMapMarkerComposable(
                    state = point,
                    position = LatLng(latAnim, lngAnim),
                    canTapOnMarker = true,
                    zIndex = 20f,
                    onClick = {
                        onEvent(LocationHistoryUiEvent.OnMarkerClick(it))
                    }
                )
            }
        }

        LocationHistoryDataViewType.Stats -> {}
    }
}

@Composable
private fun HistoryPins(
    showAllPins: Boolean,
    isPinClusteringOn: Boolean,
    selectedLocations: List<LocationHistoryPointState>,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    if (showAllPins) {
        if (isPinClusteringOn) {
            ClusteredHistoryPins(
                selectedLocations = selectedLocations,
                onEvent = onEvent
            )
        } else {
            UnclusteredHistoryPins(
                selectedLocations = selectedLocations,
                onEvent = onEvent
            )
        }
    } else {
        HistoryStartStopPointPins(
            selectedLocations = selectedLocations,
            onEvent = onEvent
        )
    }
}

@Composable
private fun ClusteredHistoryPins(
    selectedLocations: List<LocationHistoryPointState>,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    val clusterPoints = remember(selectedLocations) {
        selectedLocations.mapNotNull {
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
            } catch (e: Exception) {
            }
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
        clusterContentAnchor = Offset(0.5f, 0.5f),
        clusterItemContent = {
            // Google Map Helper composable effect to set anchoring
            ClusteringMarkerProperties(
                anchor = it.getAnchor()
            )
            HistoryMapIcon(
                isAccurate = !it.item.accuracy.isLocationInaccurate(),
                isClustered = it.item is LocationHistoryPointState.Cluster
            )
        }
    )
}

@Composable
private fun UnclusteredHistoryPins(
    selectedLocations: List<LocationHistoryPointState>,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    selectedLocations.forEach { pin ->
        if (pin.lat != null && pin.lng != null) {
            GoogleMapHistoryMapPin(
                state = pin,
                position = LatLng(pin.lat!!, pin.lng!!),
                zIndex = 1f,
                onClick = {
                    onEvent(LocationHistoryUiEvent.OnPointClicked(it))
                }
            )
        }
    }
}