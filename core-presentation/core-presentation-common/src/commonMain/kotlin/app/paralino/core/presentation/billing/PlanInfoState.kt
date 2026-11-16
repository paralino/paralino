package app.paralino.core.presentation.billing

import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanType

data class PlanInfoState(
    val productId: String,
    val name: String,
    val config: PlanConfig,
    val isCurrentPlan: Boolean,
    val isSelected: Boolean,
    val isExpanded: Boolean,
    val billingPeriods: List<PlanBillingPeriodState>
) {
    val type: PlanType = config.planTypeEnum
    val selectedBillingPeriod: PlanBillingPeriodState? =
        billingPeriods.firstOrNull { it.isSelected }

    val displayedName: String = name
}