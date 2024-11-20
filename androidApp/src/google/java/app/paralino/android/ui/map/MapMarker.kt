package app.paralino.android.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.applyCanvas
import app.paralino.android.R
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.map.MapMarkerContent
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState


@Composable
fun MapMarkerComposable(
    state: MapPinData,
    position: LatLng,
    onClick: (MapPinData) -> Unit
) {
    val markerState = MarkerState(position = position)

    MarkerInfoWindow(
        state = markerState,
        tag = state.markerId,
        anchor = when (state.markerStyle) {
            MapMarkerStyle.PIN -> Offset(0.5f, 1f)
            MapMarkerStyle.CIRCLE -> Offset(0.5f, 0.5f)
        },
        onClick = {
            if (it.isInfoWindowShown) {
                it.hideInfoWindow()
            } else {
                it.showInfoWindow()
            }
            onClick(state)
            true
        },
        infoWindowAnchor = Offset(0.5f, 0f),
        icon = mapMarkerIcon(state = state)
    ) {
        MapMarkerInfoWindowContent(state = state)
    }
}

@Composable
private fun mapMarkerIcon(
    state: MapPinData,
): BitmapDescriptor {
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
fun MapMarkerInfoWindowContent(
    state: MapPinData
) {
    val timeShow = !state.timeInfo.isNullOrBlank()
    val accuracyInfoShow =
        state.pinType == MapPinType.MEMBER && state.markerStyle == MapMarkerStyle.CIRCLE && !state.title.isNullOrBlank()
    if (timeShow || accuracyInfoShow) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .background(
                    color = MaterialTheme.colors.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(percent = 25)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            if (accuracyInfoShow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = CoreAppIcons.CircleWithOutline.asPainterResource(),
                        tint = AppColors.accentDark.copy(alpha = 0.8f),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = MR.strings.group_members_marker_circle_info.localizedString(
                            state.title ?: MR.strings.group_member_device_title.localizedString()
                        )
                    )
                }
            }

            if (accuracyInfoShow && timeShow) {
                Divider(
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (timeShow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = CoreAppIcons.AccessTime.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 2.dp, end = 4.dp)
                            .size(14.dp)
                    )
                    Text(
                        text = state.timeInfo ?: "",
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberComposeBitmapDescriptor(
    vararg keys: Any,
    content: @Composable () -> Unit,
): BitmapDescriptor {
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
): BitmapDescriptor {
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

    val bitmap =
        Bitmap
            .createBitmap(
                composeView.measuredWidth,
                composeView.measuredHeight,
                Bitmap.Config.ARGB_8888,
            )

    bitmap.applyCanvas { composeView.draw(this) }

    parent.removeView(composeView)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}