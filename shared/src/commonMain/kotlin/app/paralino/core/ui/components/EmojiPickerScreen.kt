package app.paralino.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.ImageModel


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EmojiBottomSheetPicker(
    modifier: Modifier = Modifier,
    state: EmojiBottomSheetPickerState,
    onClick: (ImageModel) -> Unit
) {
    ModalBottomSheetLayout(
        modifier = modifier.fillMaxSize(),
        sheetBackgroundColor = MaterialTheme.colors.background,
        sheetShape = RoundedCornerShape(topStartPercent = 5, topEndPercent = 5),
        sheetState = state.bottomSheetState,
        scrimColor = MaterialTheme.colors.surface.copy(alpha = 0.6f),
        sheetContent = {
            Spacer(modifier = Modifier.height(1.dp))
            EmojiPickerScreen(
                images = state.emojis,
                onClick = onClick
            )
        },
        content = {}
    )
}

class EmojiBottomSheetPickerState @OptIn(ExperimentalMaterialApi::class) constructor(
    val bottomSheetState: ModalBottomSheetState,
    initialEmojis: List<ImageModel>
) {
    var emojis by mutableStateOf(initialEmojis)

    @OptIn(ExperimentalMaterialApi::class)
    val isVisible: Boolean get() = bottomSheetState.isVisible

    @OptIn(ExperimentalMaterialApi::class)
    suspend fun show(emojis: List<String>) {
        this.emojis = emojis.map { ImageModel.emoji(it) }
        bottomSheetState.show()
    }

    @OptIn(ExperimentalMaterialApi::class)
    suspend fun hide() {
        bottomSheetState.hide()
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberEmojiBottomSheetPickerState(
    initialValue: ModalBottomSheetValue = ModalBottomSheetValue.Hidden,
    emojis: List<ImageModel> = emptyList()
): EmojiBottomSheetPickerState {
    return key(initialValue, emojis) {
        EmojiBottomSheetPickerState(
            bottomSheetState = rememberModalBottomSheetState(
                initialValue = initialValue,
                skipHalfExpanded = false
            ),
            initialEmojis = emojis
        )
    }
}

@Composable
private fun EmojiPickerScreen(
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