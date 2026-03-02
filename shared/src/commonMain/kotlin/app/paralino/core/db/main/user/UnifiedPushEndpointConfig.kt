package app.paralino.core.db.main.user

import kotlinx.serialization.Serializable

@Serializable
data class UnifiedPushEndpointConfig(
    val distributor: String,
    val url: String,
    val pubkey: String?,
    val auth: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UnifiedPushEndpointConfig

        if (distributor != other.distributor) return false
        if (url != other.url) return false
        if (pubkey != other.pubkey) return false
        if (auth != other.auth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = distributor.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (pubkey?.hashCode() ?: 0)
        result = 31 * result + (auth?.hashCode() ?: 0)
        return result
    }
}
