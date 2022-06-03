package com.zbapp.locationtrack.ui.components

import androidx.compose.runtime.Composable
import com.zbapp.core.models.base.ImageModel
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.utils.Emojis
import com.zbapp.locationtrack.ui.groups.GroupPickerScreen
import com.zbapp.locationtrack.ui.shared.emoji.EmojiPickerScreen

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