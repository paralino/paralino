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
    val newVersionAndroid: Long = 0,
    val minVersionIOS: Long = 0,
    val minUiVersionIOS: Long = 0,
    val newVersionIOS: Long = 0,
    val androidAppUrl: String? = null,
    val emailEnabled: Boolean = false,
    val plans: List<PlanConfig> = emptyList()
) {

    val isEmailEnabled: Boolean get() = AppBuildConfig.instance.isDevMode || (AppBuildConfig.instance.hasPreviewFeatures && emailEnabled)

    companion object {
        fun init(nodes: List<RemoteConfigEntity>): ConfigData {
            val map = nodes.associateBy { it.id }.mapValues { it.value.value }
            return ConfigData(
                minVersionAndroid = map.get("minVersionAndroid")?.toLongOrNull() ?: 0,
                minUiVersionAndroid = map.get("minUiVersionAndroid")?.toLongOrNull() ?: 0,
                newVersionAndroid = map.get("newVersionAndroid")?.toLongOrNull() ?: 0,
                minVersionIOS = map.get("minVersionIOS")?.toLongOrNull() ?: 0,
                minUiVersionIOS = map.get("minUiVersionIOS")?.toLongOrNull() ?: 0,
                newVersionIOS = map.get("newVersionIOS")?.toLongOrNull() ?: 0,
                androidAppUrl = map.get("androidAppUrl")?.takeIf { it.isNotBlank() },
                emailEnabled = map.get("emailEnabled")?.toBooleanStrictOrNull() ?: false,
                plans = map.get("plans")?.let {
                    try {
                        BaseRemoteModule.formatter.decodeFromString(it)
                    } catch (e: Exception) {
                        logg.e("Error parsing plans from remote: ${e.message}")
                        emptyList()
                    }
                } ?: emptyList()
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
            .filter { !it.planTypeEnum.isPremium && it.isActive }
            .maxByOrNull { it.planTypeEnum.order }
            ?: PlanConfig.default
    }

    fun listedPlanConfigs(): List<PlanConfig> {
        val freePlan = defaultFreePlanConfig()
        return listOf(freePlan) + plans.filter { it.planTypeEnum.isPremium && it.isActive }
    }
}
