package app.paralino.core.models.base

import app.paralino.core.db.main.common.RemoteConfigEntity
import app.paralino.core.logg
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    val minVersionAndroid: Long = 0,
    val minUiVersionAndroid: Long = 0,
    val minSoftVersionAndroid: Long = 0,
    val newVersionAndroid: Long = 0,
    val newSoftVersionAndroid: Long = 0,
    val minVersionIOS: Long = 0,
    val minUiVersionIOS: Long = 0,
    val minSoftVersionIOS: Long = 0,
    val newVersionIOS: Long = 0,
    val newSoftVersionIOS: Long = 0,
    val androidAppUrl: String? = null,
    val emailEnabled: Boolean = false,
    val freePlanPromoBillingId: String? = null,
    val defaultPremiumPlan: String,
    val plans: List<PlanConfig> = emptyList(),
    /**
     * _Inclusive_
     * Declares from how many active locations, clients should use bulk api
     *
     * 0 or less -> disabled,
     * 1 -> enabled always,
     * 2 or more -> only for 2 or more (inclusive)
     *
     * i.e. if value is 2, clients should use bulk api if actively updating 2 or more locations
     */
    val bulkLocationApiThreshold: Int = 0,
) {

    val isEmailEnabled: Boolean get() = AppBuildConfig.instance.isDevMode || emailEnabled

    companion object {
        fun init(nodes: List<RemoteConfigEntity>): ConfigData {
            val map = nodes.associateBy { it.id }.mapValues { it.value.value }
            return ConfigData(
                minVersionAndroid = map.get("minVersionAndroid")?.toLongOrNull() ?: 0,
                minUiVersionAndroid = map.get("minUiVersionAndroid")?.toLongOrNull() ?: 0,
                minSoftVersionAndroid = map.get("minSoftVersionAndroid")?.toLongOrNull() ?: 0,
                newVersionAndroid = map.get("newVersionAndroid")?.toLongOrNull() ?: 0,
                newSoftVersionAndroid = map.get("newSoftVersionAndroid")?.toLongOrNull() ?: 0,
                minVersionIOS = map.get("minVersionIOS")?.toLongOrNull() ?: 0,
                minUiVersionIOS = map.get("minUiVersionIOS")?.toLongOrNull() ?: 0,
                minSoftVersionIOS = map.get("minSoftVersionIOS")?.toLongOrNull() ?: 0,
                newVersionIOS = map.get("newVersionIOS")?.toLongOrNull() ?: 0,
                newSoftVersionIOS = map.get("newSoftVersionIOS")?.toLongOrNull() ?: 0,
                androidAppUrl = map.get("androidAppUrl")?.takeIf { it.isNotBlank() },
                emailEnabled = map.get("emailEnabled")?.toBooleanStrictOrNull() ?: false,
                freePlanPromoBillingId = map.get("freePlanPromoBillingId")?.takeIf { it.isNotBlank() },
                defaultPremiumPlan = map.get("defaultPremiumPlan")
                    ?.let {
                        PlanType.fromKey(key = it).takeIf { it !is PlanType.UNKNOWN }?.key
                    }
                    ?: PlanType.PRO.key,
                plans = map.get("plans")?.let {
                    try {
                        BaseRemoteModule.formatter.decodeFromString<List<PlanConfig>?>(it)
                    } catch (e: Exception) {
                        logg.e("Error parsing plans from remote: ${e.message}")
                        emptyList()
                    }
                } ?: emptyList(),
                bulkLocationApiThreshold = map.get("bulkLocationApiThreshold")?.toIntOrNull() ?: 0,
            )
        }
    }

    fun planConfigForPlanTypeOrDefault(planType: String): PlanConfig {
        return planConfigForPlanType(planType) ?: defaultFreePlanConfig()
    }

    fun planConfigForPlanType(planType: String): PlanConfig? {
        return plans.firstOrNull { it.planType == planType }
    }

    fun defaultFreePlanConfig(): PlanConfig {
        return plans
            .filter { !it.isPremium && it.isActive }
            .maxByOrNull { it.planOrder }
            ?: PlanConfig.default
    }

    fun listedPlanConfigs(): List<PlanConfig> {
        val freePlan = defaultFreePlanConfig()
        return listOf(freePlan) + plans.filter { it.isPremium && it.isActive }
    }
}

@Serializable
data class BillingConfigData(
    val dialogOffer: SubscriptionOfferDialogContent? = null,
    val subscriptionNotice: List<SubscriptionInfoNotice>? = null
)

@Serializable
data class SubscriptionOfferDialogContent(
    val requiresTrial: Boolean,
    val emoji: String?,
    val i18nTitle: Map<String, String>?,
    val i18nMessage: Map<String, String>?
)

@Serializable
data class SubscriptionInfoNotice(
    val keys: List<SubscriptionInfoNoticeValue>,
    val i18nMessage: Map<String, String>?,
)

@Serializable
data class SubscriptionInfoNoticeValue(
    val plans: List<String>,
    val periods: List<String>,
)
