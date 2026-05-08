package app.paralino.core.presentation.personal_space.settings

import app.paralino.core.presentation.common.MainAlertDialog

data class PersonalSpaceSettingsScreenState(
    val loadingAction: Boolean,
    val hasDevFeatures: Boolean,
    val alertDialog: MainAlertDialog?
)

sealed interface PersonalSpaceSettingsUiEvent {
    data object None : PersonalSpaceSettingsUiEvent
}

