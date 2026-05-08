package app.paralino.core.presentation.settings.billing

import app.paralino.core.presentation.billing.FeatureTableItemState
import app.paralino.core.presentation.billing.FeatureTableItemValue
import app.paralino.core.presentation.billing.FeatureTableState
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.MainAlertDialog

data class SubscriptionScreenState(
    val allActivePlans: List<SubscriptionDetailsState> = emptyList(),
    val alertDialog: MainAlertDialog? = null,
    val isLoading: Boolean = true,
    val currentPlanName: String,
    val featureTableState: FeatureTableState
) {
    val hasPremiumPlan: Boolean = allActivePlans.any { it.isPremiumPlan }
    val canChangePlanOnThisDevice: Boolean = if (allActivePlans.size == 1) {
        allActivePlans.first().canChangePlanOnThisDevice
    } else {
        false
    }
    val canManagePlanOnThisDevice: Boolean = allActivePlans.any { it.canManagePlanOnThisDevice == true }
}

data class SubscriptionDetailsState(
    val planId: String,
    val isCurrentPlan: Boolean,
    val isPremiumPlan: Boolean,
    val canManagePlanOnThisDevice: Boolean?,
    val canChangePlanOnThisDevice: Boolean,
    val isManageLoading: Boolean,
    val planName: String = "---",
    val billingPeriod: String?,
    val nextRenewal: String?,
    val expiresAt: String?,
    val planProviderText: String?,
)

sealed class SubscriptionScreenEvent() {
    data object OnNavigateBack : SubscriptionScreenEvent()
    data class OpenDirectPlanManagementPage(val url: String) : SubscriptionScreenEvent()
    data object OpenNativeBillingStorePage : SubscriptionScreenEvent()
}

sealed class SubscriptionScreenUiEvent() {
    data class DialogAction(val action: AlertDialogAction) : SubscriptionScreenUiEvent()
    data class OnCellItemClick(val table: FeatureTableItemState, val cellItem: FeatureTableItemValue) : SubscriptionScreenUiEvent()
    data class OnManagePlanClick(val planId: String) : SubscriptionScreenUiEvent()
}