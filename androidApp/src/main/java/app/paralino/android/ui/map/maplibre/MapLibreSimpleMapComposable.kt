package app.paralino.android.ui.map.maplibre

import android.view.MotionEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import app.paralino.core.models.location.LatLng
import app.paralino.core.models.settings.MapsSDKProvider
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.theme.LocalScreenDimensionType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.maplibre.compose.map.LocalMapState
import org.maplibre.compose.map.MapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position


@Composable
fun MapLibreSimpleMapComposable(
    modifier: Modifier = Modifier,
    mapsSDKProvider: MapsSDKProvider,
    markers: List<MapPinData>,
    fallbackCenterLatLng: LatLng?,
    zoomLevel: Double,
    canTapOnCircles: Boolean,
    isSatelliteMode: Boolean,
    isSystemInDarkTheme: Boolean,
    onMapCameraSettled: (LatLng) -> Unit,
    onMapInteraction: (Boolean) -> Unit,
    onMapTap: (lat: Double, lng: Double) -> Unit,
    onMapLongPress: (lat: Double, lng: Double) -> Unit,
) {

    val scope = rememberCoroutineScope()
    var mapLoaded by remember { mutableStateOf(false) }

    val isSmallScreenSize = LocalScreenDimensionType.current.isSmall
    val screenPadding = remember(isSmallScreenSize) {
        PaddingValues(
            if (isSmallScreenSize) 12.dp else 50.dp
        )
    }

    val mapState = rememberMapLibreMapState(
        mapsSDKProvider = mapsSDKProvider,
        isSatelliteMode = isSatelliteMode,
        isSystemInDarkTheme = isSystemInDarkTheme,
        content = {
            val currentMapState = checkNotNull(LocalMapState.current)
            markers.forEach { model ->
                val latValue = animateFloatAsState(
                    targetValue = model.lat.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "latAnim"
                ).value.toDouble()
                val lngValue = animateFloatAsState(
                    targetValue = model.lng.toFloat(),
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "lngAnim"
                ).value.toDouble()
                val accuracyAnim = animateFloatAsState(
                    targetValue = model.accuracy,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "accuracyAnim"
                ).value

                if (model.shouldShowApproximateCircle) {
                    MapLibreMapCircle(
                        markerId = model.markerId,
                        lat = latValue,
                        lng = lngValue,
                        accuracy = accuracyAnim,
                        pinType = model.pinType,
                        isFocused = model.focused,
                        isSatelliteMode = isSatelliteMode,
                        onClick = {
                            currentMapState.refocus(
                                scope = scope,
                                markers = markers,
                                padding = screenPadding
                            )
                        }
                    )
                }

                val source = rememberGeoJsonSource(
                    data = remember(latValue, lngValue) {
                        GeoJsonData.Features(
                            Point(longitude = lngValue, latitude = latValue)
                        )
                    }
                )

                MapLibreMarkerComposable(
                    state = model,
                    source = source,
                    canTapOnMarker = true,
                    zIndex = 20f,
                    onClick = {
                        currentMapState.refocus(
                            scope = scope,
                            markers = markers,
                            padding = screenPadding
                        )
                    }
                )
            }
        }
    )
    val mapController = rememberMapLibreSimpleMapController(
        mapContentPadding = remember { PaddingValues() },
        mapState = mapState
    )

    if (mapLoaded) {
        LaunchedEffect(screenPadding) {
            if (markers.isEmpty() && fallbackCenterLatLng != null) {
                mapState.setCameraPosition(
                    mapState.cameraPosition.copy(
                        center = Position(
                            latitude = fallbackCenterLatLng.lat,
                            longitude = fallbackCenterLatLng.lng,
                        ),
                        zoom = zoomLevel
                    )
                )
            } else {
                mapState.refocus(
                    scope = scope,
                    markers = markers,
                    padding = screenPadding,
                    durationMs = 0
                )
            }
        }
    }

    LaunchedEffect(key1 = markers) {
        if (mapLoaded) {
            mapState.refocus(scope = scope, markers = markers, padding = screenPadding)
        }
    }

    LaunchedEffect(key1 = mapState) {
        snapshotFlow { mapState.isCameraMoving }
            .distinctUntilChanged()
            .collectLatest {
                try {
                    onMapCameraSettled(
                        LatLng(
                            lat = mapState.cameraPosition.center.latitude,
                            lng = mapState.cameraPosition.center.longitude,
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
        GenericMapLibreMapWrapper(
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
            onMapAttributionExpanded = {},
            placeholder = {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.onBackground
                )
            }
        )
    }
}

@Composable
private fun rememberMapLibreSimpleMapController(
    mapContentPadding: PaddingValues,
    mapState: MapState
): MapLibreMapUiControlledData {
    return remember(mapState, mapContentPadding) {
        MapLibreMapUiControlledData(
            mapState = mapState,
            contentPadding = mapContentPadding,
            isCompactAttribution = true
        )
    }
}