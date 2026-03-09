package app.paralino.android.ui.map.maplibre

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Point

private const val MIN_ANIMATION_DURATION_MS = 500
private const val MAX_ANIMATION_DURATION_MS = 3000
private const val SHORT_DISTANCE_METERS = 100.0
private const val LONG_DISTANCE_METERS = 1000.0

/**
 * Calculates animation duration based on distance.
 * Short distances (< 100m) for smooth animation.
 * Long distances (> 1000m) for quick jumps.
 * Distances in between are scaled linearly.
 */
private fun calculateAnimationDuration(distanceMeters: Double): Int {
    return when {
        distanceMeters <= SHORT_DISTANCE_METERS -> MAX_ANIMATION_DURATION_MS
        distanceMeters >= LONG_DISTANCE_METERS -> MIN_ANIMATION_DURATION_MS
        else -> {
            val ratio = (distanceMeters - SHORT_DISTANCE_METERS) / (LONG_DISTANCE_METERS - SHORT_DISTANCE_METERS)
            (MAX_ANIMATION_DURATION_MS - (ratio * (MAX_ANIMATION_DURATION_MS - MIN_ANIMATION_DURATION_MS))).toInt()
        }
    }
}

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

    val animProgress = remember { Animatable(0f) }
    val acc = remember { Animatable(0f) }

    var latValue by remember { mutableDoubleStateOf(0.0) }
    var lngValue by remember { mutableDoubleStateOf(0.0) }

    val zIndex = when (model.pinType) {
        MapPinType.MEMBER -> if (model.focused) 100f else 30f
        MapPinType.PLACE -> if (model.focused) 100f else 20f
    }

    LaunchedEffect(model.markerId) {
        animProgress.snapTo(0f)
        acc.snapTo(model.accuracy)

        latValue = model.lat
        lngValue = model.lng
    }

    LaunchedEffect(model.lat, model.lng) {
        animProgress.snapTo(0f)
        val startLat = latValue
        val startLng = lngValue

        val distance = MapLibreMapUtils.computeDistanceBetween(
            from = LatLng(startLat, startLng),
            to = LatLng(model.lat, model.lng)
        )
        val durationMs = calculateAnimationDuration(distance)
        val animationSpec = tween<Float>(durationMillis = durationMs, easing = LinearEasing)

        animProgress.animateTo(1f, animationSpec = animationSpec) {
            latValue = (model.lat - startLat) * this.value + startLat
            lngValue = (model.lng - startLng) * this.value + startLng
        }
    }

    LaunchedEffect(model.accuracy) {
        val animationSpec = tween<Float>(durationMillis = MAX_ANIMATION_DURATION_MS, easing = LinearEasing)
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
        MapLibreMapCircle(
            markerId = model.markerId,
            lat = latValue,
            lng = lngValue,
            accuracy = acc.value,
            pinType = model.pinType,
            isFocused = model.focused,
            isSatelliteMode = satelliteMode,
            onClick = { onMarkerClick(model) }
        )
    }

    MapLibreMarkerComposable(
        state = model,
        source = source,
        canTapOnMarker = true,
        zIndex = zIndex,
        onClick = onMarkerClick
    )
}