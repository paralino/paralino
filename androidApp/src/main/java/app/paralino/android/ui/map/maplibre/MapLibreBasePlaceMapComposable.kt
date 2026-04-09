package app.paralino.android.ui.map.maplibre

import android.view.MotionEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.paralino.core.models.location.LatLng
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.places.create.PlaceMapFallbackLatLng
import app.paralino.core.presentation.settings.generic.MapsSDKProvider
import app.paralino.core.ui.theme.LocalScreenDimensionType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position


@Composable
fun MapLibreBasePlaceMapComposable(
    modifier: Modifier = Modifier,
    mapsSDKProvider: MapsSDKProvider,
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

    val cameraPositionState = rememberCameraState()
    val mapController = rememberPlaceMapController(
        mapContentPadding = remember { PaddingValues() },
        cameraState = cameraPositionState,
        isSatelliteMode = isSatelliteMode,
        mapsSDKProvider = mapsSDKProvider
    )

    val isSmallScreenSize = LocalScreenDimensionType.current.isSmall
    val screenPadding = remember(isSmallScreenSize) {
        PaddingValues(
            if (isSmallScreenSize) 12.dp else 50.dp
        )
    }

    if (mapLoaded) {
        LaunchedEffect(screenPadding) {
            if (markers.isEmpty() && fallbackCenterLatLng != null) {
                cameraPositionState.position = cameraPositionState.position.copy(
                    target = Position(
                        latitude = fallbackCenterLatLng.location.lat,
                        longitude = fallbackCenterLatLng.location.lng,
                    ),
                    zoom = if (fallbackCenterLatLng.isRegion) 4.0 else 10.0
                )
            } else {
                cameraPositionState.refocus(
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
            cameraPositionState.refocus(scope = scope, markers = markers, padding = screenPadding)
        }
    }

    LaunchedEffect(key1 = cameraPositionState) {
        snapshotFlow { cameraPositionState.isCameraMoving }
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
                                cameraPositionState.refocus(
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
                        },
                        options = remember { GeoJsonOptions() }
                    )

                    MapLibreMarkerComposable(
                        state = model,
                        source = source,
                        canTapOnMarker = true,
                        zIndex = 20f,
                        onClick = {
                            cameraPositionState.refocus(
                                scope = scope,
                                markers = markers,
                                padding = screenPadding
                            )
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
    cameraState: CameraState,
    isSatelliteMode: Boolean,
    mapsSDKProvider: MapsSDKProvider?
): MapLibreMapUiControlledData {
    return remember(mapContentPadding, isSatelliteMode, mapsSDKProvider) {
        derivedStateOf {
            val options = MapOptions(
                renderOptions = RenderOptions(),
                gestureOptions = GestureOptions(),
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(
                        top = mapContentPadding.calculateTopPadding(),
                        bottom = 0.dp,
                        start = mapContentPadding.calculateStartPadding(LayoutDirection.Ltr),
                        end = mapContentPadding.calculateEndPadding(LayoutDirection.Ltr),
                    ),
                    isCompassEnabled = true,
                    compassAlignment = Alignment.TopStart,
                    attributionAlignment = Alignment.BottomEnd,
                    logoAlignment = Alignment.BottomStart,
                    isScaleBarEnabled = false
                )
            )

            MapLibreMapUiControlledData(
                options = options,
                isSatelliteMode = isSatelliteMode,
                cameraState = cameraState,
                contentPadding = mapContentPadding,
                mapsSDKProvider = mapsSDKProvider
            )
        }
    }.value
}