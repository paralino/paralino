package app.paralino.core.presentation.settings.subscriptions

import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class PaywallScreenState(
    val alertDialog: MainAlertDialog? = null,
    val isLoading: Boolean = true,
    val canShowMoreInfo: Boolean = false
) {

}

sealed class PaywallScreenEvent() {
    data object OnNavigateBack : PaywallScreenEvent()
}

sealed class PaywallScreenUiEvent() {
    data object RefreshPlans : PaywallScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : PaywallScreenUiEvent()
}