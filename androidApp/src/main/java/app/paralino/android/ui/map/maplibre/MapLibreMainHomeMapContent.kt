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
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Point

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
        data = GeoJsonData.Features(
            Point(longitude = lngValue, latitude = latValue)
        ),
        options = GeoJsonOptions()
    )

    if (model.shouldShowApproximateCircle) {
        // TODO add circle as polyline
//        CircleLayer(
//            id = "circle-${model.markerId}",
//            source = source,
//            color =
//                when (model.pinType) {
//                MapPinType.MEMBER -> const(AppColors.accentDark.copy(alpha = 0.5f))
//                MapPinType.PLACE -> {
//                    if (satelliteMode) {
//                        const(Color.Blue.copy(alpha = 0.2f))
//                    } else {
//                        if (isSystemInDarkTheme()) {
//                            const(AppColors.accent.copy(alpha = 0.15f))
//                        } else {
//                            const(Color.Blue.copy(alpha = 0.15f))
//                        }
//                    }
//                }
//            }
//            ,
//            radius = const(acc.value.dp),
//            strokeColor = when (model.pinType) {
//                MapPinType.MEMBER -> const(Color.White)
//                MapPinType.PLACE -> const(Color.Blue)
//            },
//            strokeWidth = when (model.pinType) {
//                MapPinType.MEMBER -> const(8.dp)
//                MapPinType.PLACE -> const(0.dp)
//            },
//            sortKey = const(if (model.focused) 11f else 10f),
//            onClick = {
//                onMarkerClick(model)
//                ClickResult.Pass
//            }
//        )
    }

    MapLibreMarkerComposable(
        state = model,
        source = source,
        canTapOnMarker = true,
        zIndex = zIndex,
        onClick = onMarkerClick
    )
}