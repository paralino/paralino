package app.paralino.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.ImageType
import app.paralino.core.utils.Emojis
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ImageModelImage(
    imageModel: ImageModel,
    modifier: Modifier = Modifier,
    cornerSize: Dp = 8.dp,
    useBackgroundForEmoji: Boolean = true
) {
    when (imageModel.getImageType()) {
        ImageType.emoji -> {
            val emoji = imageModel.getImageValue()!!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerSize))
                    .background(MaterialTheme.colors.surface)
                    .run {
                        if (useBackgroundForEmoji)
                            this.background(MaterialTheme.colors.surface)
                        else this
                    }.then(modifier),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource("drawable/${Emojis.getResourceFromIcon(emoji)}"),
                    contentDescription = null,
                    modifier = modifier.clip(RoundedCornerShape(cornerSize)).fillMaxSize(0.6f)
                )
            }
        }
        null -> {
            Box(modifier = modifier)
        }
    }
}
