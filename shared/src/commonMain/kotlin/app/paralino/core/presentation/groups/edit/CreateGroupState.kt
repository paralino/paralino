package app.paralino.core.presentation.groups.edit

import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.groups.components.SuggestionSectionChipData
import app.paralino.core.utils.Emojis

data class CreateGroupState(
    val isNewGroup: Boolean,
    val image: ImageModel,
    val name: String,
    val hasNameError: Boolean,
    val isLoading: Boolean,
    val alertDialog: MainAlertDialog?,
    val suggestions: List<SuggestionSectionChipData>
) {
    companion object {
        fun empty(
            suggestions: List<SuggestionSectionChipData>
        ): CreateGroupState {
            return CreateGroupState(
                isNewGroup = true,
                image = ImageModel.emoji(Emojis.groupSpecific.random()),
                name = "",
                hasNameError = false,
                isLoading = false,
                alertDialog = null,
                suggestions = suggestions
            )
        }
    }
}

sealed class CreateGroupScreenEvent() {
    data class GroupUpdated(val groupId: String): CreateGroupScreenEvent()
}

sealed interface CreateGroupScreenUiEvent {
    data object CreateGroup : CreateGroupScreenUiEvent
    data class OnGroupNameChange(val name: String) : CreateGroupScreenUiEvent
    data class OnGroupSuggestionClicked(val suggestionChipData: SuggestionSectionChipData) : CreateGroupScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : CreateGroupScreenUiEvent
}