package app.paralino.android.ui.shared.emoji

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel
import app.paralino.android.ui.components.ImageModelImage

@Composable
fun EmojiPickerScreen(
    images: List<ImageModel>,
    onClick: (ImageModel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        items(images.size) {
            val image = images[it]
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(2.dp)) {
                ImageModelImage(
                    imageModel = image,
                    useBackgroundForEmoji = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onClick(image) }
                )
            }
        }
    }
}