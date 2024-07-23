package app.paralino.core.ui.components

import app.paralino.core.presentation.groups.GroupItemState

sealed interface BottomSheetPickerType {
    data class EmojiPickerBottomSheet(val emojis: List<String>) : BottomSheetPickerType
    data class GroupPickerBottomSheet(val groups: List<GroupItemState>) : BottomSheetPickerType
}