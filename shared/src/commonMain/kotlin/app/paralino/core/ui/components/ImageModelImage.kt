package app.paralino.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.ImageType
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.painterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImageModelImage(
    imageModel: ImageModel,
    modifier: Modifier = Modifier,
    clipShape: Shape = RoundedCornerShape(8.dp),
    useBackgroundForEmoji: Boolean = true,
    colorFilter: ColorFilter? = null
) {
    when (imageModel.getImageType()) {
        ImageType.emoji -> {
            val emoji = imageModel.getImageValue()!!
            BoxWithConstraints(
                modifier = Modifier
                    .clip(clipShape)
                    .run {
                        if (useBackgroundForEmoji)
                            this.background(MaterialTheme.colors.surface)
                        else this
                    }.then(modifier),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = modifier.padding(min(minHeight, minWidth).times(0.15f)),
                    painter = painterResource(Emojis.getResourceFromIcon(emoji)),
                    contentDescription = null,
                    colorFilter = colorFilter
                )
            }
        }
        ImageType.placeholder -> {
            BoxWithConstraints(
                modifier = Modifier
                    .clip(clipShape)
                    .background(MaterialTheme.colors.surface)
                    .run {
                        if (useBackgroundForEmoji)
                            this.background(MaterialTheme.colors.surface)
                        else this
                    }.then(modifier),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = modifier.padding(min(minHeight, minWidth).times(0.15f)),
                    painter = org.jetbrains.compose.resources.painterResource(resource = DrawableResource("drawable/ic_person.xml")),
                    contentDescription = null,
                    colorFilter = colorFilter
                )
            }
        }
        null -> {
            Box(modifier = modifier)
        }
    }
}
