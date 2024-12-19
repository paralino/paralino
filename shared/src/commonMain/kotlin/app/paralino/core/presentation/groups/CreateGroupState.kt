package app.paralino.core.presentation.groups

import app.paralino.core.models.base.ImageModel
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.utils.Emojis

data class CreateGroupState(
    val isNewGroup: Boolean,
    val image: ImageModel,
    val name: String,
    val hasNameError: Boolean,
    val isLoading: Boolean,
    val alertDialog: MainAlertDialog?
) {
    companion object {
        fun empty(): CreateGroupState {
            return CreateGroupState(
                isNewGroup = true,
                image = ImageModel.emoji(Emojis.groupSpecific.random()),
                name = "",
                hasNameError = false,
                isLoading = false,
                alertDialog = null
            )
        }
    }
}

sealed class CreateGroupScreenEvent() {
    data class GroupUpdated(val groupId: String): CreateGroupScreenEvent()
}

sealed interface CreateGroupScreenUiEvent {
    data object CreateGroup : CreateGroupScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : CreateGroupScreenUiEvent
}