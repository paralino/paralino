package app.paralino.core.provider

import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig

interface IBillingProvider {
    suspend fun configure(): List<SubscriptionPlanData>

    data class SubscriptionPlanData(
        val productId: String,
        val periods: List<SubscriptionPlanPeriodData>,
        val planConfig: PlanConfig,
    )

    data class SubscriptionPlanPeriodData(
        val fullProductId: String,
        val productPlanId: String,
        val billingPeriod: PlanBillingPeriod,
        val price: Long,
        val formattedPricePerMember: String?,
        val formattedPrice: String,
        val referencePerYearPrice: Long?,
        val referencePerYearFormattedPrice: String?,
        val trialDurationDays: Int?
    )
}