package app.paralino.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.base.ImageType
import app.paralino.core.utils.Emojis
import app.paralino.mobile.ui.shared.emoji.EmojiItem
import app.paralino.mobile.ui.shared.emoji.EmojiItemData
import app.paralino.mobile.ui.theme.AppTheme
import app.paralino.mobile.utils.extensions.bitmapFromBase64

@Composable
fun ImageModelImage(
    imageModel: ImageModel,
    modifier: Modifier = Modifier,
    useBackgroundForEmoji: Boolean = true
) {
    when (imageModel.getImageType()) {
        ImageType.emoji -> {
            val emoji = imageModel.getImageValue()!!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .run {
                        if (useBackgroundForEmoji)
                            this
                                .background(MaterialTheme.colors.surface)
                        else this
                    }.then(modifier)
            ) {
                AndroidView(
                    factory = { EmojiItem(it) },
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .align(Alignment.Center),
                    update = { it.itemData = EmojiItemData(emoji) }
                )
            }
        }
        ImageType.raw -> {
            val imageContent = imageModel.getImageValue()!!.bitmapFromBase64()?.asImageBitmap()
            if (imageContent != null) {
                Image(
                    bitmap = imageContent,
                    contentDescription = null,
                    modifier = modifier.clip(RoundedCornerShape(8.dp))
                )
            }
        }
        null -> {
            Box(modifier = modifier)
        }
    }
}

@Preview
@Composable
fun MainPreview() {
    AppTheme {
        Column {
            ImageModelImage(
                ImageModel.emoji(Emojis.robot),
                Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            ImageModelImage(
                ImageModel.raw(Emojis.sampleBase64Image)
            )
        }
    }
}