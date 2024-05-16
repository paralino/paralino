package app.paralino.core.models.user

import app.paralino.core.models.base.ConfigData
import app.paralino.core.models.base.PlanConfig
import app.paralino.core.models.base.PlanType
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
    val highestPlan: UserPlan? get() = plans.filter { !it.plan.isExpired }.maxByOrNull { it.plan.planType }
    val highestPlanConfig: PlanConfig get() = highestPlan?.config ?: config.planConfigForPlanType(PlanType.default)
}

data class UserPlan(
    val plan: UserPlanRaw,
    val config: PlanConfig
)