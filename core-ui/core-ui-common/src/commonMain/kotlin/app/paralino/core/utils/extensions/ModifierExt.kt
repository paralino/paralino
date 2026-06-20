package app.paralino.core.utils.extensions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.conditional(value: Boolean, action: @Composable Modifier.() -> Modifier): Modifier {
    return if (value) {
        this.action()
    } else {
        this
    }
}

@Composable
fun Modifier.pulsate(pulseAmount: Float = 1.2f): Modifier {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = pulseAmount,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    return this.scale(scale)
}

fun Modifier.dashedDotDashedBorder(
    width: Dp = 1.dp,
    color: Color,
    shape: Shape = RoundedCornerShape(10.dp),
    dashLength: Dp = 8.dp,
    dotLength: Dp = 1.dp,
    gapLength: Dp = 3.dp,
): Modifier = drawBehind {
    val pathEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(
            dashLength.toPx(),
            gapLength.toPx(),
            dotLength.toPx(),
            gapLength.toPx(),
        ),
        phase = 0f,
    )
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline = outline,
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = pathEffect,
        ),
    )
}