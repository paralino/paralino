package app.paralino.core.presentation.settings.billing

import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class SubscriptionScreenState(
    val subscriptionState: SubscriptionInfoState? = null,
    val alertDialog: MainAlertDialog? = null,
    val isLoading: Boolean = true,
    val canShowMoreInfo: Boolean = false,
    val featureTableState: FeatureTableState
) {
    val isSubscribed: Boolean = subscriptionState != null
}

data class SubscriptionInfoState(
    val name: String,
    val expiresAt: String,
    val billingPeriod: String,
    val hasPremiumPlan: Boolean,
    val canManagePlan: Boolean,
    val canChangePlanOnDevice: Boolean,
    val isManageLoading: Boolean,
    val isFromParalino: Boolean,
)

sealed class SubscriptionScreenEvent() {
    data object OnNavigateBack : SubscriptionScreenEvent()
    data class OpenDirectPlanManagementPage(val url: String) : SubscriptionScreenEvent()
    data object OpenNativeBillingStorePage : SubscriptionScreenEvent()
}

sealed class SubscriptionScreenUiEvent() {
    data class DialogAction(val action: AlertDialogAction) : SubscriptionScreenUiEvent()
    data class OnCellItemClick(val table: FeatureTableItemState, val cellItem: FeatureTableItemValue) : SubscriptionScreenUiEvent()
    data object OnManagePlanClick : SubscriptionScreenUiEvent()
}