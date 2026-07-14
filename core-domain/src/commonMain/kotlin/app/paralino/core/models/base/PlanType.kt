package app.paralino.core.models.base

import app.paralino.core.MR
import app.paralino.core.provider.localizedString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

sealed class PlanType {
    abstract val key: String
    val name: String get() = key
    abstract val order: Int

    data object FREE : PlanType() {
        override val key: String = "free"
        override val order: Int = 10
    }

    data object SILVER : PlanType() {
        override val key: String = "silver"
        override val order: Int = 100
    }

    data object STANDARD : PlanType() {
        override val key: String = "standard"
        override val order: Int = 1000
    }

    data object PRO : PlanType() {
        override val key: String = "pro"
        override val order: Int = 2000
    }

    data object MAX : PlanType() {
        override val key: String = "max"
        override val order: Int = 3000
    }

    data class UNKNOWN(override val key: String, override val order: Int = -1) : PlanType()

    val isPremium: Boolean get() = order >= STANDARD.order

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + order
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        if (other !is PlanType) return false
        if (key != other.key) return false
        if (order != other.order) return false

        return true
    }

    companion object {
        fun fromKey(
            key: String,
            order: Int = -1
        ): PlanType {
            return when (key) {
                FREE.key -> FREE
                SILVER.key -> SILVER
                STANDARD.key -> STANDARD
                PRO.key -> PRO
                MAX.key -> MAX
                else -> UNKNOWN(key = key, order = order)
            }
        }
    }
}

enum class PlanBillingPeriod(val key: String) {
    WEEKLY(key = "weekly"),
    MONTHLY(key = "monthly"),
    ANNUALLY(key = "annually"),
    ONETIME(key = "oneTime");

    val isRenewalBased: Boolean get() = this != PlanBillingPeriod.ONETIME

    companion object {
        fun fromKey(key: String): PlanBillingPeriod? {
            return values().firstOrNull { it.key == key }
        }
    }
}

enum class PlanProvider(val key: String) {
    APPLE_APP_STORE(key = "app_store"),
    GOOGLE_PLAY_STORE(key = "play_store"),
    WEB_POLAR(key = "web_polar"),
    PARALINO(key = "paralino");

    companion object {
        fun fromKey(key: String?): PlanProvider {
            return values().firstOrNull { it.key == key } ?: PARALINO
        }
    }
}

@Serializable
data class PlanConfig(
    val planType: String,
    val planOrder: Int,
    val planName: String,
    val groupsLimit: Int,
    val membersLimit: Int,
    val devicesLimit: Int,
    val personalDevicesLimit: Int,
    val placesLimit: Int,
    val personalPlacesLimit: Int,
    val privacyZonesLimit: Int = 2,
    val historyDaysLimit: Int,
    val personalHistoryDaysLimit: Int,
    val paralinoIdsLimit: Int,
    val hasLocationRequests: Boolean,
    val hasOngoingRealtime: Boolean,
    val hasBestLocationPerformance: Boolean,
    val hasPremiumMapStyles: Boolean,
    val hasSharingAlerts: Boolean,
    val hasLowBatteryAlerts: Boolean,
    val isPaidPlan: Boolean,
    val isActive: Boolean,
    val productIds: List<String> = emptyList(),
    val productPlanIds: List<String> = emptyList(),
) {

    @Transient val planTypeEnum: PlanType = PlanType.fromKey(key = planType, order = planOrder)
    val hasUnlimitedPersonalHistory: Boolean = personalHistoryDaysLimit < 0
    val hasUnlimitedGroupHistory: Boolean = historyDaysLimit < 0
    val isPersonalHistoryEnabled: Boolean = personalHistoryDaysLimit != 0
    val isGroupHistoryEnabled: Boolean = historyDaysLimit != 0

    val adjustedPlanName: String = when (planTypeEnum) {
        PlanType.SILVER -> MR.strings.paywall_silver_plan_name.localizedString()
        PlanType.FREE -> MR.strings.paywall_free_plan_name.localizedString()
        else -> planName
    }

    companion object {
        val default = PlanConfig(
            planType = PlanType.FREE.key,
            planOrder = PlanType.FREE.order,
            planName = PlanType.FREE.name,
            groupsLimit = 1,
            membersLimit = 2,
            devicesLimit = 1,
            personalDevicesLimit = 1,
            placesLimit = 2,
            personalPlacesLimit = 1,
            privacyZonesLimit = 2,
            historyDaysLimit = 0,
            personalHistoryDaysLimit = 0,
            paralinoIdsLimit = 1,
            hasLocationRequests = false,
            hasOngoingRealtime = false,
            hasBestLocationPerformance = false,
            hasPremiumMapStyles = false,
            hasSharingAlerts = false,
            hasLowBatteryAlerts = true,
            isPaidPlan = false,
            isActive = true,
            productIds = listOf("free"),
            productPlanIds = listOf("free")
        )
    }
}