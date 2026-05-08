package app.paralino.core.utils.extensions

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

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