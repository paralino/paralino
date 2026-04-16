package app.paralino.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
            val diagonalLength = (w*w + h*h).let { sqrt(it.toDouble()).toFloat() }

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
