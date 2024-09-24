package app.paralino.core.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.max

data class BlurredCircle(
    val center: Offset,
    val radius: Float,
    val color: Color,
    val blurRadius: Float
)

fun DrawScope.drawBlurredCircles(circles: List<BlurredCircle>) {
    val maxSize = max(size.width, size.height)
    circles.forEach { circle ->
        drawBlurredCircle(
            center = Offset(circle.center.x * size.width, circle.center.y * size.height),
            radius = circle.radius * maxSize,
            color = circle.color,
            blurRadius = circle.blurRadius * maxSize
        )
    }
}

private fun DrawScope.drawBlurredCircle(
    center: Offset,
    radius: Float,
    color: Color,
    blurRadius: Float
) {
    val outerRadius = radius + blurRadius
    val gradient = RadialGradientShader(
        center = center,
        radius = outerRadius,
        colors = listOf(color, color.copy(alpha = 0f)),
        colorStops = listOf(0f, 1f)
    )

    drawCircle(
        center = center,
        radius = outerRadius,
        brush = ShaderBrush(gradient),
        blendMode = BlendMode.Plus
    )
}