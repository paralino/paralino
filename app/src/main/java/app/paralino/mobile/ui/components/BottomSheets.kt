package app.paralino.mobile.ui.components

import androidx.compose.runtime.Composable
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.dashboard.GroupItemData
import app.paralino.core.utils.Emojis
import app.paralino.mobile.ui.groups.GroupPickerScreen
import app.paralino.mobile.ui.shared.emoji.EmojiPickerScreen

sealed class BottomSheetsScreen {

    object EmojiPicker : BottomSheetsScreen() {
        @Composable
        fun MakeScreen(onClose: () -> Unit, onResult: (ImageModel) -> Unit) {
            EmojiPickerScreen(
                images = Emojis.allEmojis.map { ImageModel.emoji(it) },
                onClick = {
                    onResult(it)
                    onClose()
                }
            )
        }
    }

    data class GroupPicker(val groups: List<GroupItemData>) : BottomSheetsScreen() {
        @Composable
        fun MakeScreen(
            onClose: () -> Unit,
            onResult: (GroupItemData) -> Unit,
        ) {
            GroupPickerScreen(
                groups = groups,
                onClick = {
                    onResult(it)
                    onClose()
                }
            )
        }
    }
}

@Composable
fun SheetContent(
    screen: BottomSheetsScreen?,
    onClose: () -> Unit,
    onResult: (Any) -> Unit
) {
    when (screen) {
        is BottomSheetsScreen.EmojiPicker -> {
            screen.MakeScreen(onClose, onResult)
        }
        is BottomSheetsScreen.GroupPicker -> {
            screen.MakeScreen(onClose, onResult)
        }
        null -> {}
    }
}