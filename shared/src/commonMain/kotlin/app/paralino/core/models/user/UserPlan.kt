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
            config = config.planConfigForPlanTypeOrDefault(it.planType)
        )
    }

    val nonExpiredPlans get() = plans.filter { !it.plan.isExpired }
    val highestValidPlan: UserPlan? get() = nonExpiredPlans.maxByOrNull { it.plan.planTypeEnum.order }
    val highestValidPlanConfig: PlanConfig get() = highestValidPlan?.config ?: config.defaultFreePlanConfig()

    val groupsLimit: Int = highestValidPlan?.plan?.groupsLimitOverride?.toInt() ?: highestValidPlanConfig.groupsLimit
}

data class UserPlan(
    val plan: UserPlanRaw,
    val config: PlanConfig
)