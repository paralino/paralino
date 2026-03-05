package app.paralino.core.models.user

import app.paralino.core.utils.BaseRemoteModule
import kotlinx.serialization.Serializable

@Serializable
data class PushProviderEndpointConfig(
    val distributor: String,
    val url: String,
    val createdAt: String?,
    val pubkey: String?,
    val auth: String?,
    val isSocketConnection: Boolean?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PushProviderEndpointConfig

        if (distributor != other.distributor) return false
        if (url != other.url) return false
        if (createdAt != other.createdAt) return false
        if (pubkey != other.pubkey) return false
        if (auth != other.auth) return false
        if (isSocketConnection != other.isSocketConnection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = distributor.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (pubkey?.hashCode() ?: 0)
        result = 31 * result + (auth?.hashCode() ?: 0)
        result = 31 * result + (isSocketConnection?.hashCode() ?: 0)
        return result
    }

    companion object Companion {
        fun get(config: String?): PushProviderEndpointConfig? {
            if (config.isNullOrBlank()) {
                return null
            } else {
                return try {
                    BaseRemoteModule.formatter.decodeFromString<PushProviderEndpointConfig>(config)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}