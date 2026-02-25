package app.paralino.core.presentation.settings.billing

import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class SubscriptionScreenState(
    val subscriptionState: SubscriptionInfoState? = null,
    val allActivePlans: List<SubscriptionDetailsState> = emptyList(),
    val alertDialog: MainAlertDialog? = null,
    val isLoading: Boolean = true,
    val canShowMoreInfo: Boolean = false,
    val featureTableState: FeatureTableState
) {
    val isSubscribed: Boolean = subscriptionState != null
    val currentPlanName: String = allActivePlans.firstOrNull { it.isCurrentPlan }?.planName ?: "---"
}

data class SubscriptionInfoState(
    val canManagePlan: Boolean,
    val hasPremiumPlan: Boolean,
    val canChangePlanOnDevice: Boolean,
    val isManageLoading: Boolean,
    val isFromParalino: Boolean,
)

data class SubscriptionDetailsState(
    val isCurrentPlan: Boolean = false,
    val canManagePlan: Boolean = true,
    val planName: String = "---",
    val billingPeriod: String? = null,
    val validUntil: String? = null,
    val planProviderText: String? = null
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