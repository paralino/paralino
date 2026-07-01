package app.paralino.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.DeviceType
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.provider.current
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.Emojis

@Composable
fun MapMarkerContent(
    modifier: Modifier = Modifier,
    state: MapPinData
) {
    val pinSize = remember(state) {
        state.getPinSize().dp
    }
    val pxSize = with(LocalDensity.current) { pinSize.toPx() }

    Box(
        modifier = modifier.size(pinSize),
        contentAlignment = Alignment.TopCenter
    ) {
        if (state.markerStyle == MapMarkerStyle.PIN) {
            val pinPath = remember(pxSize) {
                createPinPath(pxSize)
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                drawPath(
                    path = pinPath,
                    color = if (state.active) AppColors.accentDark else Color.DarkGray
                )
            }
        }
        val maxSize = when (state.markerStyle) {
            MapMarkerStyle.PIN -> 0.9f
            MapMarkerStyle.CIRCLE -> 1f
        }
        Box(
            modifier = Modifier.size(pinSize * maxSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(
                        pinSize * when (DeviceType.current) {
                            DeviceType.Android -> 0.8f
                            DeviceType.iOS -> 0.7f
                        }
                    )
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // when rendering on iOS size is not the same, probably because of bubble image from moko
                ImageModelImage(
                    modifier = Modifier.matchParentSize(),
                    imageModel = state.image ?: ImageModel.emoji(Emojis.lock),
                    clipShape = CircleShape,
                    useBackgroundForEmoji = false,
                    colorFilter = if (!state.active) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else {
                        null
                    }
                )
            }
        }

        when (state.pinType) {
            MapPinType.PRIVACY_ZONE -> {
                Icon(
                    modifier = Modifier
                        .size(pinSize / 2)
                        .background(
                            color = MaterialTheme.colors.background,
                            shape = CircleShape
                        )
                        .background(
                            color = AppColors.accentDark.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .padding(1.dp)
                        .align(Alignment.BottomEnd),
                    painter = CoreAppIcons.PrivacyZone.asPainterResource(),
                    tint = AppColors.accentDark,
                    contentDescription = null
                )
            }

            MapPinType.MEMBER,
            MapPinType.PLACE -> {}
        }
    }
}

private fun createPinPath(totalSize: Float): Path {
    val pointerHeight = totalSize * 0.1f
    val circleRadius = (totalSize / 2f) * 0.9f
    val circleCenter = Offset(totalSize / 2f, circleRadius)
    val circleBottomY = circleCenter.y + circleRadius

    // Circle path
    val circlePath = Path().apply {
        addOval(
            Rect(
                left = circleCenter.x - circleRadius,
                top = circleCenter.y - circleRadius,
                right = circleCenter.x + circleRadius,
                bottom = circleCenter.y + circleRadius
            )
        )
    }

    // Triangle path
    val triangleHalfWidth = circleRadius * 0.45f
    val triangleOverlap = pointerHeight * 0.8f
    val trianglePath = Path().apply {
        moveTo(totalSize / 2f, circleBottomY + pointerHeight)
        lineTo(totalSize / 2f - triangleHalfWidth, circleBottomY - triangleOverlap)
        lineTo(totalSize / 2f + triangleHalfWidth, circleBottomY - triangleOverlap)
        close()
    }

    return Path.combine(
        operation = PathOperation.Union,
        path1 = circlePath,
        path2 = trianglePath
    )
}
