package app.paralino.core.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import dev.icerock.moko.resources.ImageResource

@Composable
expect fun mokoPainterResource(imageResource: ImageResource): Painter