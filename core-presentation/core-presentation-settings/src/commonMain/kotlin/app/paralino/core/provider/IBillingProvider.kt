package app.paralino.core.provider

import app.paralino.core.models.base.BillingConfigData
import app.paralino.core.models.base.PlanBillingPeriod
import app.paralino.core.models.base.PlanConfig

interface IBillingProvider {
    val isExternalProvider: Boolean
    suspend fun configure(): SubscriptionProviderData
    suspend fun startDirectPurchase(url: String)

    data class SubscriptionProviderData(
        val plans: List<SubscriptionPlanData>,
        val billingConfig: BillingConfigData?,
        val canCloseOnboardingPaywall: Boolean
    )

    data class SubscriptionPlanData(
        val productId: String,
        val periods: List<SubscriptionPlanPeriodData>,
        val planConfig: PlanConfig
    )

    data class SubscriptionPlanPeriodData(
        val fullProductId: String,
        val productPlanId: String,
        val billingPeriod: PlanBillingPeriod,
        val price: Long,
        val formattedPricePerMemberPerMonth: String?,
        val formattedPrice: String,
        val referencePerYearPrice: Long?,
        val referencePerYearFormattedPrice: String?,
        val referencePerMonthFormattedPrice: String?,
        val trialDurationDays: Int?
    )
}