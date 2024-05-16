package app.paralino.core.models.base

import kotlinx.serialization.Serializable


enum class PlanType(
    val key: String,
) {
    FREE(key = "free"),
    SILVER(key = "silver"),
    PRO(key = "pro"),
    MAX(key = "max");

    val defaultConfig: PlanConfig
        get() {
            return when (this) {
                FREE -> PlanConfig(
                    planType = FREE,
                    groupsLimit = 1,
                    membersLimit = 5,
                    devicesLimit = 1,
                    placesLimit = 2,
                    historyDaysLimit = 2,
                    paralinoIdsLimit = 1,
                    personalDevicesLimit = 2
                )

                SILVER -> PlanConfig(
                    planType = SILVER,
                    groupsLimit = 2,
                    membersLimit = 10,
                    devicesLimit = 2,
                    placesLimit = 3,
                    historyDaysLimit = 2,
                    paralinoIdsLimit = 2,
                    personalDevicesLimit = 2
                )

                PRO -> PlanConfig(
                    planType = PRO,
                    groupsLimit = 10,
                    membersLimit = 20,
                    devicesLimit = 3,
                    placesLimit = 100,
                    historyDaysLimit = 30,
                    paralinoIdsLimit = 5,
                    personalDevicesLimit = 10
                )

                MAX -> PlanConfig(
                    planType = MAX,
                    groupsLimit = 100,
                    membersLimit = 50,
                    devicesLimit = 10,
                    placesLimit = 1000,
                    historyDaysLimit = -1,
                    paralinoIdsLimit = 10,
                    personalDevicesLimit = 100
                )
            }
        }

    companion object {
        val default get() = SILVER
        fun fromKey(key: String): PlanType? {
            return entries.firstOrNull { it.key == key }
        }
    }
}

enum class PlanBillingPeriod(val key: String) {
    MONTHLY(key = "monthly"),
    ANNUALLY(key = "annually"),
    ONETIME(key = "oneTime");

    companion object {
        fun fromKey(key: String): PlanBillingPeriod? {
            return values().firstOrNull { it.key == key }
        }
    }
}

@Serializable
data class PlanConfig(
    val planType: PlanType,
    val groupsLimit: Int,
    val membersLimit: Int,
    val devicesLimit: Int,
    val placesLimit: Int,
    val historyDaysLimit: Int,
    val paralinoIdsLimit: Int,
    val personalDevicesLimit: Int
) {
    // null or -1 for override are equal to no override
    // allow override default limits within maximum range (respect maximum limit)

    fun canCreateGroup(count: Long, limitOverride: Long?): Boolean {
        return count < (limitOverride?.takeIf { it in 1..1000 } ?: groupsLimit.toLong())
    }

    fun canCreatePlace(count: Long, limitOverride: Long?): Boolean {
        return count < (limitOverride?.takeIf { it in 1..1000 } ?: placesLimit.toLong())
    }

    fun canCreateMember(count: Long, limitOverride: Long?): Boolean {
        return count < (limitOverride?.takeIf { it in 1..100 } ?: membersLimit.toLong())
    }

    fun canCreateDevice(count: Long, limitOverride: Long?): Boolean {
        return count < (limitOverride?.takeIf { it in 1..100 } ?: devicesLimit.toLong())
    }

    fun canCreatePersonalDevice(count: Long, limitOverride: Long?): Boolean {
        return count < (limitOverride?.takeIf { it in 1..100 } ?: personalDevicesLimit.toLong())
    }

    fun canCreateParalinoId(count: Long): Boolean {
        return count < paralinoIdsLimit
    }
}