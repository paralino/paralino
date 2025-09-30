package app.paralino.core.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.presentation.map.MapHomeScreenUiEvent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PersonalSpaceFeatureIntroContent(
    modifier: Modifier = Modifier,
    onEvent: (MapHomeScreenUiEvent) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = AppColors.accentLight.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
    ) {
        WavyDashedLine(
            modifier = Modifier
                .matchParentSize(),
            lineColor = AppColors.accent.copy(alpha = 0.1f),
            dotColor = AppColors.accent.copy(alpha = 0.1f),
            waveAngle = 35f
        )

        Icon(
            painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(50.dp)
                .offset(x = (-10).dp, y = (-10).dp)
                .rotate(160f),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )

        Icon(
            painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(80.dp)
                .rotate(20f)
                .offset(x = 30.dp, y = (-10).dp),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )

        Icon(
            painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(70.dp)
                .rotate(-10f)
                .offset(x = (-30).dp, y = 10.dp),
            tint = AppColors.accent.copy(alpha = 0.1f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            DefaultToolbar(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(MR.strings.personalSpace_sheet_introBanner_title),
                elevation = 0.dp,
                color = Color.Transparent,
                contentColor = MaterialTheme.colors.onBackground,
                navigationIcon = {
                    ToolbarButtons.Close { onEvent(MapHomeScreenUiEvent.ConfirmPersonalSpaceIntro) }
                }
            )

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                text = stringResource(MR.strings.personalSpace_sheet_introBanner_message),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )
        }
    }
}

@Composable
fun WavyDashedLine(
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF2196F3),
    dotColor: Color = Color.White,
    lineStrokeWidth: Dp = 2.dp,
    dashLength: Dp = 10.dp,
    dashGap: Dp = 8.dp,
    amplitude: Dp = 16.dp,
    frequency: Float = 1.2f,
    samples: Int = 240,
    dotRadius: Dp = 4.dp,
    dotPositions: List<Float> = listOf(0.25f, 0.75f),
    waveAngle: Float = 0f // in degrees; 0 = horizontal, 90 = vertical
) {
    val density = LocalDensity.current
    val strokePx = with(density) { lineStrokeWidth.toPx() }
    val dashPx = with(density) { dashLength.toPx() }
    val gapPx = with(density) { dashGap.toPx() }
    val ampPx = with(density) { amplitude.toPx() }
    val radiusPx = with(density) { dotRadius.toPx() }

    val angleRad = waveAngle * PI.toFloat() / 180f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Compute start/end points based on angle
            val diagonalLength = (w*w + h*h).let { kotlin.math.sqrt(it.toDouble()).toFloat() }

            val start = Offset(w/2 - diagonalLength/2*cos(angleRad), h/2 - diagonalLength/2*sin(angleRad))
            val end = Offset(w/2 + diagonalLength/2*cos(angleRad), h/2 + diagonalLength/2*sin(angleRad))

            val path = Path().apply {
                for (i in 0..samples) {
                    val t = i.toFloat() / samples.toFloat()
                    val x = start.x + t * (end.x - start.x)
                    val y = start.y + t * (end.y - start.y)
                    val wave = sin(2 * PI * frequency * t).toFloat() * ampPx
                    // Perpendicular offset
                    val dx = -wave * sin(angleRad)
                    val dy =  wave * cos(angleRad)
                    val finalX = x + dx
                    val finalY = y + dy

                    if (i == 0) moveTo(finalX, finalY) else lineTo(finalX, finalY)
                }
            }

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = strokePx, pathEffect = dashEffect)
            )

            // Draw dots
            dotPositions.forEach { pos ->
                val x = start.x + pos * (end.x - start.x)
                val y = start.y + pos * (end.y - start.y)
                val wave = sin(2 * PI * frequency * pos).toFloat() * ampPx
                val dx = -wave * sin(angleRad)
                val dy =  wave * cos(angleRad)
                drawCircle(
                    color = dotColor,
                    radius = radiusPx,
                    center = Offset(x + dx, y + dy)
                )
            }
        }
    }
}
