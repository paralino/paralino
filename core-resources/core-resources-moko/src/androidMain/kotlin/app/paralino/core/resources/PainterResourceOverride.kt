package app.paralino.core.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import dev.icerock.moko.resources.ImageResource

@Composable
actual fun mokoPainterResource(imageResource: ImageResource): Painter {
    return dev.icerock.moko.resources.compose.painterResource(imageResource = imageResource)
}