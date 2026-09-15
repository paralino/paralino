package app.paralino.android.ui.map.maplibre.history

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.paralino.android.ui.map.maplibre.MapLibreMapCircle
import app.paralino.android.ui.map.maplibre.MapLibreMapUiControlledData
import app.paralino.android.ui.map.maplibre.MapLibreMarkerComposable
import app.paralino.android.ui.map.maplibre.refocus
import app.paralino.android.ui.map.maplibre.rememberComposeImageBitmap
import app.paralino.core.models.location.LocationHistoryPointState
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.history.LocationHistoryDataViewType
import app.paralino.core.presentation.history.LocationHistoryScreenUiState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.isLocationInaccurate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.Feature.get
import org.maplibre.compose.expressions.dsl.Feature.has
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.expressions.dsl.step
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.SourceDefaults
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.toJson

@Composable
fun rememberMapLibreLocationHistoryMapController(
    cameraState: CameraState = rememberCameraState(),
    uiState: LocationHistoryScreenUiState,
    mapContentPadding: PaddingValues,
    isSatelliteMode: Boolean,
    isMapLoaded: Boolean,
    mapsSDKProvider: MapsSDKProvider?,
    animateFirstContent: Boolean,
    onMapTouch: () -> Unit
): MapLibreMapUiControlledData {
    val stateData by rememberUpdatedState(newValue = uiState)
    val scope = rememberCoroutineScope()
    var canAnimateMapFocus by remember { mutableStateOf(animateFirstContent) }
    var shouldAnimateMapFocus by rememberSaveable { mutableStateOf(false) }

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
        val extraVerticalPadding = if (stateData.canCollapseMapHeader) 12.dp else 60.dp
        val extraHorizontalPadding = if (stateData.canCollapseMapHeader) 40.dp else 60.dp
        val mapPadding = PaddingValues(
            start = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr) + extraHorizontalPadding,
            top = mapContentPadding.calculateTopPadding() + extraVerticalPadding,
            end = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr) + extraHorizontalPadding,
            bottom = mapContentPadding.calculateBottomPadding() + extraVerticalPadding
        )
        snapshotFlow { stateData.focusType }
            .distinctUntilChanged()
            .filter { it.isInOverview }
            .collectLatest {
//                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
//                    delay(20)
//                }
                cameraState.refocus(
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
                    padding = mapPadding
                )
            }
    }

    LaunchedEffect(
        stateData.selectedMapPinData,
        stateData.canCollapseMapHeader,
        stateData.focusType,
        mapContentPadding
    ) {
        val extraPadding = if (stateData.canCollapseMapHeader) 20.dp else 100.dp
        val mapPadding = PaddingValues(
            start = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr) + extraPadding,
            top = mapContentPadding.calculateTopPadding() + extraPadding,
            end = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr) + extraPadding,
            bottom = mapContentPadding.calculateBottomPadding() + extraPadding
        )
        snapshotFlow { stateData.focusType }
            .filterNotNull()
            .distinctUntilChanged()
            .filter { it.isMember }
            .collectLatest {
                if (!canAnimateMapFocus && shouldAnimateMapFocus) {
                    delay(20)
                } else {
                    delay(50)
                }

                cameraState.refocus(
                    scope = scope,
                    markers = listOfNotNull(stateData.selectedMapPinData),
                    ignoreAccuracy = false,
                    durationMs = if (canAnimateMapFocus) 300 else 0,
                    forceZoom = it.isMemberFocused,
                    padding = mapPadding
                )
            }
    }

    LaunchedEffect(key1 = cameraState) {
        snapshotFlow { cameraState.isCameraMoving }
            .distinctUntilChanged()
            .filter { it && cameraState.moveReason == CameraMoveReason.GESTURE }
            .collectLatest { onMapTouch() }
    }

    return remember(mapContentPadding, isSatelliteMode, mapsSDKProvider) {
        derivedStateOf {
            val options = MapOptions(
                renderOptions = RenderOptions(),
                gestureOptions = GestureOptions()
            )

            MapLibreMapUiControlledData(
                options = options,
                isSatelliteMode = isSatelliteMode,
                cameraState = cameraState,
                contentPadding = mapContentPadding,
                mapsSDKProvider = mapsSDKProvider,
                isCompactAttribution = false
            )
        }
    }.value
}

@Composable
fun MapLibreLocationHistoryMapContent(
    uiState: LocationHistoryScreenUiState,
    isSatelliteMode: Boolean,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    HistoryPolyline(
        uiState = uiState,
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
        isSatelliteMode = isSatelliteMode,
        onEvent = onEvent
    )
}

@Composable
private fun HistoryPolyline(
    uiState: LocationHistoryScreenUiState,
    isSatelliteMode: Boolean,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    if (uiState.showPolyline && uiState.selectedLocations.size >= 2) {
        val polylineSource = rememberGeoJsonSource(
            data = remember(uiState.selectedLocations) {
                GeoJsonData.Features(
                    LineString(
                        coordinates = uiState.selectedLocations.mapNotNull {
                            when (it) {
                                is LocationHistoryPointState.Cluster -> {
                                    Position(longitude = it.lng, latitude = it.lat)
                                }

                                is LocationHistoryPointState.Point -> {
                                    it.point.loc?.let {
                                        Position(longitude = it.lng, latitude = it.lat)
                                    }
                                }
                            }
                        }
                    )
                )
            },
            options = remember { GeoJsonOptions() }
        )

        LineLayer(
            id = "history-path",
            source = polylineSource,
            color = if (isSatelliteMode || isSystemInDarkTheme()) {
                const(AppColors.redMain)
            } else {
                const(MaterialTheme.colors.primary)
            },
            width = const(5.dp),
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
            onClick = {
                onEvent(LocationHistoryUiEvent.ChangeMapDataViewType(LocationHistoryDataViewType.LocationPoint))
                ClickResult.Consume
            }
        )
    }
}

@Composable
private fun HistoryStartStopPointPins(
    selectedLocations: List<LocationHistoryPointState>,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    val startStopPoints = remember(selectedLocations) {
        val labeledPoints: List<Pair<LocationHistoryPointState, String>> = when {
            selectedLocations.isEmpty() -> emptyList()
            selectedLocations.size == 1 -> listOf(selectedLocations.first() to "A")
            else -> listOf(
                selectedLocations.first() to "A",
                selectedLocations.last() to "B"
            )
        }
        GeoJsonData.JsonString(
            FeatureCollection(
                features = labeledPoints.mapNotNull { (point, content) ->
                    val lat = point.lat
                    val lng = point.lng
                    if (lat != null && lng != null) {
                        Feature(
                            id = JsonPrimitive(point.pointId),
                            geometry = Point(
                                Position(
                                    longitude = lng,
                                    latitude = lat
                                )
                            ),
                            properties = JsonObject(
                                mapOf(
                                    "pointId" to JsonPrimitive(point.pointId),
                                    "isStartPoint" to JsonPrimitive(content == "A"),
                                )
                            )
                        )
                    } else {
                        null
                    }
                }
            ).toJson()
        )
    }

    val startStopSource = rememberGeoJsonSource(
        data = startStopPoints,
        options = remember { GeoJsonOptions() }
    )
    MapLibreHistoryStartStopMarkerComposable(
        source = startStopSource,
        canTapOnMarker = true,
        zIndex = 1f,
        onClick = {
            onEvent(LocationHistoryUiEvent.OnPointIdClicked(it))
        }
    )
}

@Composable
private fun SelectedHistoryPointMarker(
    dataViewType: LocationHistoryDataViewType,
    selectedMarker: MapPinData?,
    isSatelliteMode: Boolean,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    when (dataViewType) {
        LocationHistoryDataViewType.LocationPoint -> {
            selectedMarker?.let { point ->

                val latValue = animateFloatAsState(
                    targetValue = point.lat.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "latAnim"
                ).value.toDouble()
                val lngValue = animateFloatAsState(
                    targetValue = point.lng.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "lngAnim"
                ).value.toDouble()
                val accuracyAnim = animateFloatAsState(
                    targetValue = point.accuracy,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "accuracyAnim"
                ).value

                val source = rememberGeoJsonSource(
                    data = remember(latValue, lngValue) {
                        GeoJsonData.Features(
                            Point(longitude = lngValue, latitude = latValue)
                        )
                    },
                    options = remember { GeoJsonOptions() }
                )

                if (point.shouldShowApproximateCircle) {
                    MapLibreMapCircle(
                        markerId = point.markerId,
                        lat = latValue,
                        lngValue,
                        accuracy = accuracyAnim,
                        pinType = point.pinType,
                        isFocused = point.focused,
                        isSatelliteMode = isSatelliteMode,
                        onClick = { onEvent(LocationHistoryUiEvent.OnMarkerClick(point)) }
                    )
                }

                MapLibreMarkerComposable(
                    state = point,
                    source = source,
                    canTapOnMarker = true,
                    zIndex = 20f,
                    onClick = { onEvent(LocationHistoryUiEvent.OnMarkerClick(it)) }
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
    val geoData = remember(selectedLocations) {
        GeoJsonData.JsonString(
            FeatureCollection(
                features = selectedLocations.mapNotNull { point ->
                    if (point.lat != null && point.lng != null) {
                        Feature(
                            id = JsonPrimitive(point.pointId),
                            geometry = Point(
                                Position(
                                    longitude = point.lng!!,
                                    latitude = point.lat!!
                                )
                            ),
                            properties = JsonObject(
                                mapOf(
                                    "pointId" to JsonPrimitive(point.pointId),
                                    "isClustered" to JsonPrimitive(point is LocationHistoryPointState.Cluster),
                                    "isInaccurate" to JsonPrimitive(
                                        (point as? LocationHistoryPointState.Point)
                                            ?.accuracy?.isLocationInaccurate()
                                            ?: false
                                    ),
                                )
                            )
                        )
                    } else {
                        null
                    }
                }
            ).toJson()
        )
    }

    if (showAllPins) {
        if (isPinClusteringOn && selectedLocations.isNotEmpty()) {
            ClusteredHistoryPins(
                geoData = geoData,
                onEvent = onEvent
            )
        } else {
            UnclusteredHistoryPins(
                geoData = geoData,
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
    geoData: GeoJsonData.JsonString,
    onEvent: (LocationHistoryUiEvent) -> Unit,
) {
    val clusterSource = rememberGeoJsonSource(
        data = geoData,
        options = remember {
            GeoJsonOptions(
                cluster = true,
                clusterRadius = 32,
//                        clusterMinPoints = 1,
//                        clusterMaxZoom = 24,
                maxZoom = SourceDefaults.MAX_ZOOM,
                clusterProperties = mapOf(
                    "pointId" to GeoJsonOptions.ClusterPropertyAggregator(
                        mapper = get("pointId"),
                        reducer = get("pointId")
                    )
                )
            )
        }
    )

    SymbolLayer(
        id = "history-clusters",
        source = clusterSource,
        iconImage = step(
            get("point_count").asNumber(),
            fallback = image(clusterIconBitmap(size = 8.dp)),
            10 to image(clusterIconBitmap(size = 12.dp)),
            100 to image(clusterIconBitmap(size = 18.dp)),
            1000 to image(clusterIconBitmap(size = 24.dp)),
        ),
        filter = has("point_count"),
        iconAllowOverlap = const(true),
        textAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        textIgnorePlacement = const(true),
        onClick = { features ->
            features.firstOrNull()?.let { feature ->
                feature.properties?.get("pointId")?.jsonPrimitive?.content?.let {
                    onEvent(LocationHistoryUiEvent.OnPointIdClicked(it))
                }
                ClickResult.Consume
            } ?: ClickResult.Pass
        }
    )

    SymbolLayer(
        id = "history-cluster-count",
        source = clusterSource,
        filter = has("point_count"),
        textField = get("point_count_abbreviated").asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = const(AppColors.darkBackground),
        iconAllowOverlap = const(true),
        textAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        textIgnorePlacement = const(true),
    )

    MapLibreHistoryMarkerComposable(
        source = clusterSource,
        canTapOnMarker = true,
        zIndex = 1f,
        filter = has("point_count").not(),
        onClick = {
            onEvent(LocationHistoryUiEvent.OnPointIdClicked(it))
        }
    )
}

@Composable
private fun UnclusteredHistoryPins(
    geoData: GeoJsonData.JsonString,
    onEvent: (LocationHistoryUiEvent) -> Unit,
) {
    val source = rememberGeoJsonSource(
        data = geoData,
        options = remember { GeoJsonOptions() }
    )
    MapLibreHistoryMarkerComposable(
        source = source,
        canTapOnMarker = true,
        zIndex = 1f,
        onClick = {
            onEvent(LocationHistoryUiEvent.OnPointIdClicked(it))
        }
    )
}

@Composable
private fun clusterIconBitmap(
    modifier: Modifier = Modifier,
    size: Dp
): ImageBitmap {
    return rememberComposeImageBitmap {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.highlightGold)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space for the count text rendered by MapLibre
            Box(modifier = Modifier.size(size, 12.dp))
        }
    }
}