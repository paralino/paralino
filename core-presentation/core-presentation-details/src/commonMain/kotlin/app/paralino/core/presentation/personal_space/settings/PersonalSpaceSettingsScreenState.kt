package app.paralino.core.presentation.personal_space.settings

import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog

data class PersonalSpaceSettingsScreenState(
    val loadingAction: Boolean,
    val hasDevFeatures: Boolean,
    val alertDialog: MainAlertDialog?
)

sealed interface PersonalSpaceSettingsUiEvent {
    data object DisablePersonalSpaceClick : PersonalSpaceSettingsUiEvent
    data class DialogAction(val action: AlertDialogAction) : PersonalSpaceSettingsUiEvent
}

sealed interface PersonalSpaceSettingsEvent {
    data object OpenMap : PersonalSpaceSettingsEvent
}

