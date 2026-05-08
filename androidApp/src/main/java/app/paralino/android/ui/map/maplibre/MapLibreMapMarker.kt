package app.paralino.android.ui.map.maplibre

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.ui.components.MapMarkerContent
import app.paralino.core.ui.theme.AppColors
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.Source
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position


@Composable
fun MapLibreMarkerComposable(
    state: MapPinData,
    lat: Double,
    lng: Double,
    canTapOnMarker: Boolean,
    zIndex: Float,
    onClick: (MapPinData) -> Unit
) {
    MapLibreMarkerComposable(
        state = state,
        source = GeoJsonSource(
            id = state.markerId,
            data = GeoJsonData.Features(
                Point(longitude = lng, latitude = lat)
            ),
            options = GeoJsonOptions()
        ),
        canTapOnMarker = canTapOnMarker,
        zIndex = zIndex,
        onClick = onClick
    )
}

@Composable
fun MapLibreMarkerComposable(
    state: MapPinData,
    source: Source,
    canTapOnMarker: Boolean,
    zIndex: Float,
    onClick: (MapPinData) -> Unit
) {
    SymbolLayer(
        id = "map-marker-${state.markerId}",
        source = source,
        iconImage = image(mapMarkerIcon(state)),
        iconAllowOverlap = const(true),
        textAllowOverlap = const(true),
        iconIgnorePlacement = const(true),
        textIgnorePlacement = const(true),
        onClick = if (canTapOnMarker) {
            {
                onClick(state)
                ClickResult.Consume
            }
        } else {
            null
        },
        sortKey = const(zIndex),
        iconAnchor = when (state.markerStyle) {
            MapMarkerStyle.PIN -> const(SymbolAnchor.Bottom)
            MapMarkerStyle.CIRCLE -> const(SymbolAnchor.Center)
        }
    )
}

@Composable
fun MapLibreMapCircle(
    markerId: String,
    lat: Double,
    lng: Double,
    accuracy: Float,
//    canTapOnMarker: Boolean,
    pinType: MapPinType,
    isFocused: Boolean,
    isSatelliteMode: Boolean,
    onClick: () -> Unit
) {
    val circlePolygon = remember(lat, lng, accuracy) {
        buildAccuracyCirclePolygon(
            center = LatLng(latitude = lat, longitude = lng),
            radiusMeters = accuracy.toDouble()
        )
    }

    val circleSource = circlePolygon?.let {
        rememberGeoJsonSource(
            data = GeoJsonData.Features(it),
            options = GeoJsonOptions()
        )
    }

    val fillColor = when (pinType) {
        MapPinType.MEMBER -> AppColors.accentDark.copy(alpha = 0.5f)
        MapPinType.PLACE -> {
            if (isSatelliteMode) {
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

    val strokeColor = when (pinType) {
        MapPinType.MEMBER -> Color.White
        MapPinType.PLACE -> Color.Blue
    }

    val strokeWidth = when (pinType) {
        MapPinType.MEMBER -> 4.dp
        MapPinType.PLACE -> 0.dp
    }

    val circleSortKey = const(if (isFocused) 11f else 10f)

    if (circleSource != null) {
        FillLayer(
            id = "circle-fill-${markerId}",
            source = circleSource,
            color = const(fillColor),
            outlineColor = const(Color.Transparent),
            sortKey = circleSortKey,
            onClick = {
                onClick()
                ClickResult.Consume
            }
        )

        if (strokeWidth > 0.dp) {
            LineLayer(
                id = "circle-stroke-${markerId}",
                source = circleSource,
                color = const(strokeColor),
                width = const(strokeWidth),
                sortKey = circleSortKey,
                onClick = {
                    onClick()
                    ClickResult.Consume
                }
            )
        }
    }
}

@Composable
private fun mapMarkerIcon(
    state: MapPinData,
): ImageBitmap {
    val keys = listOfNotNull(
        state.active,
        state.focused,
        state.image,
        state.markerStyle
    ).toTypedArray()

    return rememberComposeImageBitmap(*keys) {
        MapMarkerContent(state = state)
    }
}

@Composable
internal fun rememberComposeImageBitmap(
    vararg keys: Any,
    content: @Composable () -> Unit,
): ImageBitmap {
    val parent = LocalView.current as ViewGroup
    val compositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)

    return remember(parent, compositionContext, currentContent, *keys) {
        renderComposableToBitmapDescriptor(parent, compositionContext, currentContent)
    }
}

private fun renderComposableToBitmapDescriptor(
    parent: ViewGroup,
    compositionContext: CompositionContext,
    content: @Composable () -> Unit,
): ImageBitmap {
    val fakeCanvas = Canvas()
    val composeView =
        ComposeView(parent.context)
            .apply {
                setParentCompositionContext(compositionContext)
                setContent(content)
            }
            .also(parent::addView)

    composeView.draw(fakeCanvas)

    composeView.measure(
        View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.AT_MOST),
    )

    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

    val bitmap = Bitmap
        .createBitmap(
            composeView.measuredWidth,
            composeView.measuredHeight,
            Bitmap.Config.ARGB_8888,
        )

    bitmap.applyCanvas { composeView.draw(this) }

    parent.removeView(composeView)
    return bitmap.asImageBitmap()
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