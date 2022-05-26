package com.zbapp.locationtrack.ui.shared.emoji

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.base.ImageModel
import com.zbapp.locationtrack.ui.components.ImageModelImage

@Composable
fun EmojiPickerScreen(
    images: List<ImageModel>,
    onClick: (ImageModel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 50.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        items(images.size) {
            val image = images[it]
            Box(modifier = Modifier.padding(0.dp)) {
                ImageModelImage(
                    imageModel = image,
                    useBackgroundForEmoji = false,
                    modifier = Modifier
                        .size(50.dp)
                        .clickable { onClick(image) }
                )
            }
        }
    }
}