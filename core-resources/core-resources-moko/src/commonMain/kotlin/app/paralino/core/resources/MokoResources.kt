package app.paralino.core.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import dev.icerock.moko.resources.ImageResource

object MokoResources {
    @Composable
    fun ImageResource.imageAsPainterResource(): Painter {
        return mokoPainterResource(this)
    }
}