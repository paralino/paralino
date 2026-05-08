package app.paralino.core.presentation.billing

import app.paralino.core.models.base.PlanBillingPeriod

data class PlanBillingPeriodState(
    val productPlanId: String,
    val billingPeriod: PlanBillingPeriod,
    val name: String,
    val isSelected: Boolean,
    val price: String,
    val pricePerMonth: String?,
    val pricePerMember: String?,
    val discountedPrice: String?,
    val discountPercent: String?,
    val isPromoDiscount: Boolean,
    val trialDurationDays: Int?,
    val isNativePlatformCurrentBillingPeriod: Boolean
)