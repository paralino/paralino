package app.paralino.android.ui.map.maplibre

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.applyCanvas
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.map.MapMarkerContent
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.GeoJsonSource
import org.maplibre.compose.sources.Source
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point


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
        id = state.markerId,
        source = source,
        iconImage = image(mapMarkerIcon(state)),
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