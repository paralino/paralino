package app.paralino.core.models.user

import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.raw.UserPlanRaw

data class UserPlans(
    val rawPlans: List<UserPlanRaw>,
    val config: ConfigData
) {
    val plans = rawPlans.map {
        UserPlan(
            plan = it,
            config = config.planConfigForPlanType(it.planType)
        )
    }

    val nonExpiredPlans = plans.filter { !it.plan.isExpired }
    val highestPlan: UserPlan? get() = nonExpiredPlans.maxByOrNull { it.plan.planType }
    val highestPlanConfig: PlanConfig get() = highestPlan?.config ?: PlanConfig.default
}

data class UserPlan(
    val plan: UserPlanRaw,
    val config: PlanConfig
)