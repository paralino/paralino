package app.paralino.core.models.user

import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.raw.UserPlanRaw
import app.paralino.core.utils.extensions.nowUTC
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

data class UserPlans(
    val rawPlans: List<UserPlanRaw>,
    val config: ConfigData
) {
    val plans = rawPlans.map {
        UserPlan(
            plan = it,
            config = config.planConfigForPlanTypeOrDefault(it.planType)
        )
    }

    val nonExpiredPlans: List<UserPlan> get() = plans.filter { !it.isPlanFullyExpired(config) }
    val highestValidPlan: UserPlan? get() = nonExpiredPlans
        .groupBy { it.config.planType }
        // pick the longest lasting plan for every plan type
        .mapValues { it.value.sortedByDescending { it.plan.expiresAt?.toString() }.firstOrNull() }
        .values
        .filterNotNull()
        .maxByOrNull { it.config.planOrder }

    val highestValidPlanConfig: PlanConfig get() = highestValidPlan?.config ?: config.defaultFreePlanConfig()
    val hasActiveWebPlan: Boolean get() = nonExpiredPlans.any {
        it.plan.provider == PlanProvider.WEB_POLAR
    }

    val groupsLimit: Int = highestValidPlan?.plan?.groupsLimitOverride?.toInt() ?: highestValidPlanConfig.groupsLimit
}

data class UserPlan(
    val plan: UserPlanRaw,
    val config: PlanConfig
) {
    // allow for some manual grace period
    // it might take some time until correct subscription data is synced and reflected on backend
    // most of time it is just few minutes at most
    val isExpiredWithoutGracePeriod: Boolean get() = plan.expiresAt
        ?.plus(if (AppBuildConfig.instance.isDevMode) 30.seconds else 2.minutes)
        ?.let { it < nowUTC() }
        ?: false

    fun gracePeriodAdjustedExpiresAt(config: ConfigData): Instant? {
        return plan.expiresAt
            ?.plus(config.planGracePeriodFallbackSeconds.seconds)
    }

    fun isPlanFullyExpired(config: ConfigData): Boolean {
        return gracePeriodAdjustedExpiresAt(config)
            ?.let { it < nowUTC() }
            ?: false
    }
}