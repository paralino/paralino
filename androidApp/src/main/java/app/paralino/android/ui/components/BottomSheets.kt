package app.paralino.android.ui.components

import androidx.compose.runtime.Composable
import app.paralino.android.ui.groups.GroupPickerAction
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.android.ui.groups.GroupPickerScreen
import app.paralino.android.ui.shared.emoji.EmojiPickerScreen

sealed class BottomSheetsScreen {

    data class EmojiPicker(val emojis: List<String>) : BottomSheetsScreen() {
        @Composable
        fun MakeScreen(onClose: () -> Unit, onResult: (ImageModel) -> Unit) {
            EmojiPickerScreen(
                images = emojis.map { ImageModel.emoji(it) },
                onClick = {
                    onResult(it)
                    onClose()
                }
            )
        }
    }

    data class GroupPicker(val groups: List<GroupItemState>) : BottomSheetsScreen() {
        @Composable
        fun MakeScreen(
            onClose: () -> Unit,
            onResult: (GroupPickerAction) -> Unit,
        ) {
            GroupPickerScreen(
                groups = groups,
                onClose = onClose,
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