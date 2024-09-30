package app.paralino.core.presentation.settings.billing

import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanType
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.MainAlertDialog

data class PaywallScreenState(
    val plans: List<PlanInfoState>,
    val canShowMoreInfo: Boolean,
    val isLoading: Boolean,
    val isPurchaseButtonLoading: Boolean,
    val alertDialog: MainAlertDialog?,
) {
    val selectedPlan: PlanInfoState? = plans.firstOrNull { it.isSelected }
    val selectedBillingPeriod: PlanBillingPeriodState? = selectedPlan?.selectedBillingPeriod
}

data class PlanInfoState(
    val productId: String,
    val name: String,
    val type: PlanType,
    val isCurrentPlan: Boolean,
    val isSelected: Boolean,
    val isExpanded: Boolean,
    val billingPeriods: List<PlanBillingPeriodState>,
    val benefits: List<String>
) {
    val oneLineBenefits: String = benefits.joinToString(separator = ", ")
    val selectedBillingPeriod: PlanBillingPeriodState? = billingPeriods.firstOrNull { it.isSelected }
}

data class PlanBillingPeriodState(
    val productPlanId: String,
    val billingPeriod: PlanBillingPeriod,
    val name: String,
    val isSelected: Boolean,
    val price: String,
    val discountedPrice: String?,
    val discountPercent: String?,
    val isPromoDiscount: Boolean,
    val pricePerMonth: String?,
    val trialDurationDays: Int?,
)

sealed class PaywallScreenEvent() {
    data class OnStartPurchase(
        val planType: PlanType,
        val newPlanProductId: String
    ) : PaywallScreenEvent()
    data object OnRestorePurchase : PaywallScreenEvent()
    data class OnNavigateBack(val isSuccess: Boolean) : PaywallScreenEvent()
}

sealed class PaywallScreenUiEvent() {
    data object RefreshPlans : PaywallScreenUiEvent()
    data object StartPurchase : PaywallScreenUiEvent()
    data object RestorePurchase : PaywallScreenUiEvent()
    data class OnPurchaseError(val error: PurchaseErrorException) : PaywallScreenUiEvent()
    data class SelectBillingPeriod(val planType: PlanType, val period: PlanBillingPeriod) : PaywallScreenUiEvent()
    data class SelectPlanType(val planType: PlanType) : PaywallScreenUiEvent()
    data class DialogAction(val action: AlertDialogAction) : PaywallScreenUiEvent()
}

class PurchaseErrorException(
    val errorCode: Int,
    val errorMessage: String,
    val userCancelled: Boolean
): Throwable(errorMessage)