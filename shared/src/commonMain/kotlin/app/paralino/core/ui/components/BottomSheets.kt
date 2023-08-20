package app.paralino.core.ui.components

import androidx.compose.runtime.Composable
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.ui.groups.GroupPickerScreen

sealed class BottomSheetsScreen {
    data class EmojiPicker(val emojis: List<String>) : BottomSheetsScreen()
    data class GroupPicker(val groups: List<GroupItemState>) : BottomSheetsScreen()

    @Composable
    fun SheetContent(
        onClose: () -> Unit,
        onResult: (Any) -> Unit
    ) {
        when (this) {
            is BottomSheetsScreen.EmojiPicker -> {
                EmojiPickerScreen(
                    images = this.emojis.map { ImageModel.emoji(it) },
                    onClick = {
                        onResult(it)
                        onClose()
                    }
                )
            }

            is BottomSheetsScreen.GroupPicker -> {
                GroupPickerScreen(
                    groups = this.groups,
                    onClose = onClose,
                    onClick = {
                        onResult(it)
                        onClose()
                    }
                )
            }
        }
    }
}