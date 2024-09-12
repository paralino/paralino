package app.paralino.core.presentation.settings.subscriptions

import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class SubscriptionScreenState(
    val subscriptionState: SubscriptionInfoState? = null,
    val alertDialog: MainAlertDialog? = null,
    val isLoading: Boolean = true,
    val canShowMoreInfo: Boolean = false
) {
    val isSubscribed: Boolean = subscriptionState != null
}

data class SubscriptionInfoState(
    val name: String,
    val expiresAt: String,
    val billingPeriod: String,
    val hasPremiumPlan: Boolean,
    val isCurrentPlatform: Boolean,
    val isFromParalino: Boolean,
)

sealed class SubscriptionScreenEvent() {
    data object OnNavigateBack : SubscriptionScreenEvent()
}

sealed class SubscriptionScreenUiEvent() {
    data class DialogAction(val action: AlertDialogAction) : SubscriptionScreenUiEvent()
}