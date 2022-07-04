package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserCryptoKeyRaw constructor(
    @SerialName("\$id")
    val id: String,
    val ownerUserId: String,
    val encryptedSymmetricKey: String,
    val encryptedPrivateKey: String,
    val publicKey: String,
    val version: Long
) {
    fun toPostJson(): Map<String, Any?> {
        return buildMap {
            put("ownerUserId", ownerUserId)
            put("encryptedSymmetricKey", encryptedSymmetricKey)
            put("encryptedPrivateKey", encryptedPrivateKey)
            put("publicKey", publicKey)
            put("version", version)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): UserCryptoKeyRaw {
            return UserCryptoKeyRaw(
                id = map["\$id"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedSymmetricKey = map["encryptedSymmetricKey"] as String,
                encryptedPrivateKey = map["encryptedPrivateKey"] as String,
                publicKey = map["publicKey"] as String,
                version = map["version"] as Long
            )
        }
    }
}
