package app.paralino.core.models.base

import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    val minVersionAndroid: Long,
    val minUiVersionAndroid: Long,
    val newVersionAndroid: Long,
    val minVersionIOS: Long,
    val minUiVersionIOS: Long,
    val newVersionIOS: Long,
    val androidAppUrl: String?,
    val emailEnabled: Boolean,
    val planFree: PlanConfig,
    val planBeta: PlanConfig,
    val planPro: PlanConfig,
    val planMax: PlanConfig
) {
    companion object {
        fun init(nodes: List<ConfigNode>): ConfigData {
            return ConfigData(
                minUiVersionAndroid = nodes.firstOrNull { it.id == "min_ui_version_android" }?.value?.toLongOrNull() ?: 0,
                minVersionAndroid = nodes.firstOrNull { it.id == "min_version_android" }?.value?.toLongOrNull() ?: 0,
                newVersionAndroid = nodes.firstOrNull { it.id == "new_version_android" }?.value?.toLongOrNull() ?: 0,
                minVersionIOS = nodes.firstOrNull { it.id == "min_version_ios" }?.value?.toLongOrNull() ?: 0,
                minUiVersionIOS = nodes.firstOrNull { it.id == "min_ui_version_ios" }?.value?.toLongOrNull() ?: 0,
                newVersionIOS = nodes.firstOrNull { it.id == "new_version_ios" }?.value?.toLongOrNull() ?: 0,
                androidAppUrl = nodes.firstOrNull { it.id == "android_app_url" }?.value?.takeIf { it.isNotBlank() },
                emailEnabled = nodes.firstOrNull { it.id == "email_enabled" }?.value?.toBoolean() ?: false,
                planFree = nodes.firstOrNull { it.id == "plan_config_free" }?.value?.let {
                    try {
                        BaseRemoteModule.formatter.decodeFromString<PlanConfigValue>(it).toPlanConfig(PlanType.FREE)
                    } catch (e: Exception) {
                        null
                    }
                } ?: PlanType.FREE.defaultConfig,
                planBeta = nodes.firstOrNull { it.id == "plan_config_beta" }?.value?.let {
                    try {
                        BaseRemoteModule.formatter.decodeFromString<PlanConfigValue>(it).toPlanConfig(PlanType.SILVER)
                    } catch (e: Exception) {
                        null
                    }
                } ?: PlanType.SILVER.defaultConfig,
                planPro = nodes.firstOrNull { it.id == "plan_config_pro" }?.value?.let {
                    try {
                        BaseRemoteModule.formatter.decodeFromString<PlanConfigValue>(it).toPlanConfig(PlanType.PRO)
                    } catch (e: Exception) {
                        null
                    }
                } ?: PlanType.PRO.defaultConfig,
                planMax = nodes.firstOrNull { it.id == "plan_config_max" }?.value?.let {
                    try {
                        BaseRemoteModule.formatter.decodeFromString<PlanConfigValue>(it).toPlanConfig(PlanType.MAX)
                    } catch (e: Exception) {
                        null
                    }
                } ?: PlanType.MAX.defaultConfig
            )
        }
    }

    fun planConfigForPlanType(planType: PlanType): PlanConfig {
        return when (planType) {
            PlanType.SILVER -> planBeta
            PlanType.FREE -> planFree
            PlanType.PRO -> planPro
            PlanType.MAX -> planMax
        }
    }
}

@Serializable
data class ConfigNode(
    @SerialName("\$id")
    val id: String,
    val value: String?
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): ConfigNode {
            return ConfigNode(
                id = map["\$id"] as String,
                value = map["value"] as? String?,
            )
        }
    }
}

private data class PlanConfigValue(
    val groups: Int,
    val members: Int,
    val devices: Int,
    val places: Int,
    val history: Int,
    val paralinoIds: Int,
    val personalDevices: Int
) {
    fun toPlanConfig(type: PlanType): PlanConfig {
        return PlanConfig(
            planType = type,
            groupsLimit = groups,
            membersLimit = members,
            devicesLimit = devices,
            placesLimit = places,
            historyDaysLimit = history,
            paralinoIdsLimit = paralinoIds,
            personalDevicesLimit = personalDevices
        )
    }
}