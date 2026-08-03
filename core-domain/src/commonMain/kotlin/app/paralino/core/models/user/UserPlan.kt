package app.paralino.core.models.user

import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanProvider
import app.paralino.core.models.raw.UserPlanRaw

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

    val nonExpiredPlans: List<UserPlan> get() = plans.filter { !it.plan.isExpired }
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
)