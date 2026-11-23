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
    val discount: String?,
    val trialDurationDays: Int?,
    val introOffer: IntroOfferState?,
    val isNativePlatformCurrentlyActiveBillingPeriod: Boolean
)

data class IntroOfferState(
    val price: String,
    val durationMonths: Int,
    // null for single payment
    val recurringPeriodMonths: Int?
)
