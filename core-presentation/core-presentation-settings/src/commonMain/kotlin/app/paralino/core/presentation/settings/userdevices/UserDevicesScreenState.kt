package app.paralino.core.presentation.settings.userdevices

import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog
import app.paralino.core.presentation.personal_space.UserDeviceItemEvent
import app.paralino.core.presentation.personal_space.UserDeviceOrSessionItemState

data class UserDevicesScreenState(
    val isLoadingData: Boolean,
    val loadingAction: Boolean,
    val devices: List<UserDeviceOrSessionItemState>,
    val alertDialog: MainAlertDialog?,
)

sealed interface UserDevicesScreenUiEvent {
    data class OnDeviceEvent(val event: UserDeviceItemEvent) : UserDevicesScreenUiEvent
    data class DialogAction(val action: AlertDialogAction) : UserDevicesScreenUiEvent
}
