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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.ImageType
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.compose.painterResource

@Composable
fun ImageModelImage(
    imageModel: ImageModel,
    modifier: Modifier = Modifier,
    clipShape: Shape = RoundedCornerShape(8.dp),
    useBackgroundForEmoji: Boolean = true
) {
    when (imageModel.getImageType()) {
        ImageType.emoji -> {
            val emoji = imageModel.getImageValue()!!
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
                    painter = painterResource(Emojis.getResourceFromIcon(emoji)),
                    contentDescription = null,
                    modifier = modifier.padding(min(minHeight, minWidth).times(0.15f))
                )
            }
        }
        null -> {
            Box(modifier = modifier)
        }
    }
}
