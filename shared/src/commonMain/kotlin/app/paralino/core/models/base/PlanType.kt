package app.paralino.core.models.base

import kotlinx.serialization.Serializable


sealed class PlanType {
    abstract val key: String
    val name: String get() = key

    data object FREE : PlanType() {
        override val key: String = "free"
    }
    data object SILVER : PlanType() { override val key: String = "silver" }
    data object PRO : PlanType() { override val key: String = "pro" }
    data object MAX : PlanType() { override val key: String = "max" }
    data class UNKNOWN(override val key: String) : PlanType()

    companion object {
        fun fromKey(key: String): PlanType {
            return when (key) {
                FREE.key -> FREE
                SILVER.key -> SILVER
                PRO.key -> PRO
                MAX.key -> MAX
                else -> UNKNOWN(key)
            }
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
    val planType: String,
    val planName: String,
    val groupsLimit: Int,
    val membersLimit: Int,
    val devicesLimit: Int,
    val placesLimit: Int,
    val historyDaysLimit: Int,
    val paralinoIdsLimit: Int,
    val personalDevicesLimit: Int
) {
    companion object {
        val default = PlanConfig(
            planType = PlanType.FREE.key,
            planName = PlanType.FREE.name,
            groupsLimit = 1,
            membersLimit = 5,
            devicesLimit = 1,
            placesLimit = 2,
            historyDaysLimit = 2,
            paralinoIdsLimit = 1,
            personalDevicesLimit = 2
        )
    }
}