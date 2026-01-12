package app.paralino.android.ui.map.maplibre

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.ui.theme.AppColors
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapLibreMainHomeMapContent(
    uiState: MapHomeScreenState.Ready?,
    onEvent: (MapHomeScreenUiEvent) -> Unit
) {
    uiState?.visibleMarkers?.forEach { model ->
        MapHomeMarkerComposable(
            model = model,
            satelliteMode = uiState.isSatelliteMode,
            onMarkerClick = { onEvent(MapHomeScreenUiEvent.OnMarkerClick(it)) }
        )
    }
}

@Composable
private fun MapHomeMarkerComposable(
    model: MapPinData,
    satelliteMode: Boolean,
    onMarkerClick: (MapPinData) -> Unit
) {

    val lat = remember { Animatable(0f) }
    val lng = remember { Animatable(0f) }
    val acc = remember { Animatable(0f) }

    var latValue by remember { mutableDoubleStateOf(0.0) }
    var lngValue by remember { mutableDoubleStateOf(0.0) }

    val animationSpec = remember {
        tween<Float>(durationMillis = 4000, easing = LinearEasing)
    }

    val zIndex = when (model.pinType) {
        MapPinType.MEMBER -> if (model.focused) 40f else 30f
        MapPinType.PLACE -> if (model.focused) 40f else 20f
    }

    LaunchedEffect(model.markerId) {
        lat.snapTo(0f)
        lng.snapTo(0f)
        acc.snapTo(model.accuracy)

        latValue = model.lat
        lngValue = model.lng
    }

    LaunchedEffect(model.lat) {
        lat.snapTo(0f)
        val startLat = latValue
        lat.animateTo(1f, animationSpec = animationSpec) {
            latValue = (model.lat - startLat) * this.value + startLat
        }
    }

    LaunchedEffect(model.lng) {
        lng.snapTo(0f)
        val startLng = lngValue
        lng.animateTo(1f, animationSpec = animationSpec) {
            lngValue = (model.lng - startLng) * this.value + startLng
        }
    }

    LaunchedEffect(model.accuracy) {
        acc.animateTo(model.accuracy, animationSpec = animationSpec)
    }

    val source = rememberGeoJsonSource(
        data = remember(latValue, lngValue) {
            GeoJsonData.Features(
                Point(longitude = lngValue, latitude = latValue)
            )
        },
        options = remember { GeoJsonOptions() }
    )

    if (model.shouldShowApproximateCircle) {
        val circlePolygon = remember(latValue, lngValue, acc.value) {
            buildAccuracyCirclePolygon(
                center = LatLng(latValue, lngValue),
                radiusMeters = acc.value.toDouble()
            )
        }

        val circleSource = circlePolygon?.let {
            rememberGeoJsonSource(
                data = GeoJsonData.Features(it),
                options = GeoJsonOptions()
            )
        }

        val fillColor = when (model.pinType) {
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
            }

        val strokeColor = when (model.pinType) {
                MapPinType.MEMBER -> Color.White
                MapPinType.PLACE -> Color.Blue
            }

        val strokeWidth = when (model.pinType) {
                MapPinType.MEMBER -> 4.dp
                MapPinType.PLACE -> 0.dp
            }

        val circleSortKey = const(if (model.focused) 11f else 10f)

        if (circleSource != null) {
            FillLayer(
                id = "circle-fill-${model.markerId}",
                source = circleSource,
                color = const(fillColor),
                outlineColor = const(Color.Transparent),
                sortKey = circleSortKey,
                onClick = {
                    onMarkerClick(model)
                    ClickResult.Consume
                }
            )

            if (strokeWidth > 0.dp) {
                LineLayer(
                    id = "circle-stroke-${model.markerId}",
                    source = circleSource,
                    color = const(strokeColor),
                    width = const(strokeWidth),
                    sortKey = circleSortKey,
                    onClick = {
                        onMarkerClick(model)
                        ClickResult.Consume
                    }
                )
            }
        }
    }

    MapLibreMarkerComposable(
        state = model,
        source = source,
        canTapOnMarker = true,
        zIndex = zIndex,
        onClick = onMarkerClick
    )
}

private fun buildAccuracyCirclePolygon(
    center: LatLng,
    radiusMeters: Double,
    points: Int = 64
): Polygon? {
    if (radiusMeters <= 0 || points < 3) return null
    val angleStep = 360.0 / points
    val ring = mutableListOf<Position>()

    repeat(points) { index ->
        val heading = index * angleStep
        val target = MapLibreMapUtils.computeOffset(center, radiusMeters, heading)
        ring.add(Position(longitude = target.longitude, latitude = target.latitude))
    }

    // Close the ring by repeating the first coordinate
    ring.add(ring.first())

    return Polygon(coordinates = listOf(ring))
}