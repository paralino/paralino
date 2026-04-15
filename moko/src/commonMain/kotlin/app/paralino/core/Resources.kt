package app.paralino.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import dev.icerock.moko.resources.ImageResource

object MR {
    val strings = MokoResources.strings
    val images = MokoResources.images
    object fonts {
        val font_montserrat_light = MokoResources.fonts.font_montserrat_light
        val font_montserrat_regular = MokoResources.fonts.font_montserrat_regular
        val font_montserrat_medium = MokoResources.fonts.font_montserrat_medium
        val font_montserrat_semibold = MokoResources.fonts.font_montserrat_semibold
        val font_montserrat_bold = MokoResources.fonts.font_montserrat_bold
        val font_montserrat_extrabold = MokoResources.fonts.font_montserrat_extrabold
    }
    val plurals = MokoResources.plurals
}

@Composable
fun ImageResource.asPainterResource(): Painter {
    return dev.icerock.moko.resources.compose.painterResource(this)
}