package app.paralino.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.conditional
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun MapActionBubbleMarkerContent(
    modifier: Modifier = Modifier,
    positionOnScreen: IntOffset,
    showPrivacyZoneButton: Boolean,
    onCreatePlace: () -> Unit,
    onCreatePrivacyZone: () -> Unit,
) {
    val density = LocalDensity.current
    val pointerHeight = 10.dp
    val pointerDotRadius = 3.dp
    val cornerRadiusPx = with(density) { 22.dp.toPx() }
    val pointerHeightPx = with(density) { pointerHeight.toPx() }
    val pointerHalfWidthPx = with(density) { 8.dp.toPx() }
    val pointerDotRadiusPx = with(density) { pointerDotRadius.toPx() }
    val borderWidthPx = with(density) { 2.dp.toPx() }
    val bubbleBackground = MaterialTheme.colors.background
    val bubbleColor = AppColors.accentDark.copy(alpha = 0.2f)

    var boxSize by remember { mutableStateOf<IntSize?>(null) }
    var offset by remember { mutableStateOf<IntOffset?>(null) }

    Box(
        modifier = modifier
            .conditional(offset != null) {
                offset?.let { offset ->
                    offset { offset }
                } ?: this
            }
            .onGloballyPositioned {
                boxSize = it.size
                offset = IntOffset(
                    x = positionOnScreen.x - it.size.width / 2,
                    y = positionOnScreen.y - it.size.height,
                )
            }
            .alpha(if (offset != null) 1f else 0f),
        contentAlignment = Alignment.TopCenter
    ) {

        if (boxSize != null) {
            val popupPath = remember(boxSize, cornerRadiusPx, pointerHeightPx, pointerHalfWidthPx) {
                createMapLongTapPopupPath(
                    width = boxSize!!.width.toFloat(),
                    height = boxSize!!.height.toFloat(),
                    cornerRadius = cornerRadiusPx,
                    pointerHeight = pointerHeightPx,
                    pointerHalfWidth = pointerHalfWidthPx
                )
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                drawPath(
                    path = popupPath,
                    color = bubbleBackground,
                )
                drawPath(
                    path = popupPath,
                    color = bubbleColor,
                )
                drawPath(
                    path = popupPath,
                    color = bubbleBackground,
                    style = Stroke(width = borderWidthPx),
                )

                val pointerTip = Offset(
                    x = boxSize!!.width / 2f,
                    y = boxSize!!.height.toFloat(),
                )
                drawCircle(
                    color = AppColors.redMain,
                    radius = pointerDotRadiusPx,
                    center = pointerTip,
                )
                drawCircle(
                    color = bubbleBackground,
                    radius = pointerDotRadiusPx,
                    center = pointerTip,
                    style = Stroke(width = borderWidthPx),
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(bottom = pointerHeight)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallActionButton(
                modifier = Modifier,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                text = {
                    Text(
                        text = stringResource(MR.strings.map_long_tap_action_new_place)
                    )
                },
                onClick = onCreatePlace
            )

            if (showPrivacyZoneButton) {
                SmallActionButton(
                    modifier = Modifier,
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary,
                    icon = CoreAppIcons.PrivacyZone.asPainterResource(),
                    text = {
                        Text(
                            text = stringResource(MR.strings.map_long_tap_action_new_privacy_zone),
                        )
                    },
                    onClick = onCreatePrivacyZone
                )
            }
        }
    }
}

private fun createMapLongTapPopupPath(
    width: Float,
    height: Float,
    cornerRadius: Float,
    pointerHeight: Float,
    pointerHalfWidth: Float,
): Path {
    val bodyBottom = height - pointerHeight
    val roundedRectPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(0f, 0f, width, bodyBottom),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }

    val centerX = width / 2f
    val pointerPath = Path().apply {
        moveTo(centerX, height)
        lineTo(centerX - pointerHalfWidth, bodyBottom)
        lineTo(centerX + pointerHalfWidth, bodyBottom)
        close()
    }

    return Path.combine(
        operation = PathOperation.Union,
        path1 = roundedRectPath,
        path2 = pointerPath
    )
}
