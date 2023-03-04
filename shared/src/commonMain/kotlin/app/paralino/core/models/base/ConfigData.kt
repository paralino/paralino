package app.paralino.core.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    val minVersionAndroid: Long,
    val newVersionAndroid: Long,
    val minVersionIOS: Long,
    val newVersionIOS: Long
) {
    companion object {
        fun init(nodes: List<ConfigNode>): ConfigData {
            return ConfigData(
                minVersionAndroid = nodes.firstOrNull { it.id == "min_version_android" }?.value?.toLongOrNull() ?: 0,
                newVersionAndroid = nodes.firstOrNull { it.id == "new_version_android" }?.value?.toLongOrNull() ?: 0,
                minVersionIOS = nodes.firstOrNull { it.id == "min_version_ios" }?.value?.toLongOrNull() ?: 0,
                newVersionIOS = nodes.firstOrNull { it.id == "new_version_ios" }?.value?.toLongOrNull() ?: 0,
            )
        }
    }
}

@Serializable
data class ConfigNode(
    @SerialName("\$id")
    val id: String,
    val value: String
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): ConfigNode {
            return ConfigNode(
                id = map["\$id"] as String,
                value = map["value"] as String,
            )
        }
    }
}