package app.paralino.android.ui.map.google

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.applyCanvas
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.ui.components.MapMarkerContent
import app.paralino.core.ui.components.MapMarkerInfoWindowContent
import app.paralino.core.ui.components.hasInfoWindowContent
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.AdvancedMarkerInfoWindow
import com.google.maps.android.compose.rememberUpdatedMarkerState


@Composable
fun GoogleMapMarkerComposable(
    state: MapPinData,
    position: LatLng,
    canTapOnMarker: Boolean,
    zIndex: Float,
    onClick: (MapPinData) -> Unit
) {
    // Workaround for maps sdk 8.5.0+ which does not measure info window properly and makes it stretched horizontally
    // TODO check if fixed upstream when new sdk is out
    val mapWidth = LocalView.current.width
    val maxInfoWindowWidth: Dp? = if (mapWidth > 0) {
        with(LocalDensity.current) { mapWidth.toDp() }
    } else {
        null
    }
    val markerState = rememberUpdatedMarkerState(position = position)
    val hasInfoWindowContent = state.hasInfoWindowContent()

    LaunchedEffect(hasInfoWindowContent) {
        if (!hasInfoWindowContent) {
            markerState.hideInfoWindow()
        }
    }

    AdvancedMarkerInfoWindow(
        state = markerState,
        tag = state.markerId,
        anchor = when (state.markerStyle) {
            MapMarkerStyle.PIN -> Offset(0.5f, 1f)
            MapMarkerStyle.CIRCLE -> Offset(0.5f, 0.5f)
        },
        onClick = {
            if (canTapOnMarker) {
                if (it.isInfoWindowShown) {
                    it.hideInfoWindow()
                } else if (hasInfoWindowContent) {
                    try {
                        it.showInfoWindow()
                    } catch (e: Exception) {
                        // swallow info window display exceptions
                    }
                }
                onClick(state)
            }
            canTapOnMarker
        },
        infoWindowAnchor = Offset(0.5f, 0f),
        icon = mapMarkerIcon(state = state),
        zIndex = zIndex,
        // maps-compose 8.5.0+ crashes if custom info window content measures 0x0
        content = if (hasInfoWindowContent) {
            {
                MapMarkerInfoWindowContent(
                    state = state,
                    modifier = if (maxInfoWindowWidth != null) {
                        Modifier.widthIn(max = maxInfoWindowWidth)
                    } else {
                        Modifier
                    }
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun mapMarkerIcon(
    state: MapPinData,
): BitmapDescriptor? {
    val keys = listOfNotNull(
        state.active,
        state.focused,
        state.image,
        state.markerStyle
    ).toTypedArray()

    return rememberComposeBitmapDescriptor(*keys) {
        MapMarkerContent(state = state)
    }
}

@Composable
internal fun rememberComposeBitmapDescriptor(
    vararg keys: Any,
    content: @Composable () -> Unit,
): BitmapDescriptor? {
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
): BitmapDescriptor? {
    val fakeCanvas = Canvas()
    val composeView = ComposeView(parent.context)
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

    if (composeView.measuredWidth == 0 || composeView.measuredHeight == 0) {
        // view not ready
        parent.removeView(composeView)
        return null
    }

    val bitmap = Bitmap.createBitmap(
        composeView.measuredWidth,
        composeView.measuredHeight,
        Bitmap.Config.ARGB_8888,
    )

    bitmap.applyCanvas { composeView.draw(this) }

    parent.removeView(composeView)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}