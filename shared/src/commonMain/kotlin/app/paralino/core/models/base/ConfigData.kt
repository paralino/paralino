package app.paralino.core.models.base

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
    val alternativeIDsEnabled: Boolean
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
                alternativeIDsEnabled = nodes.firstOrNull { it.id == "alternative_paralinoid_enabled" }?.value?.toBoolean() ?: false,
            )
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