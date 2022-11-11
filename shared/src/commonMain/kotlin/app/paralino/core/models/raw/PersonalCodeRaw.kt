package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalCodeRaw constructor(
    @SerialName("\$id")
    val id: String,
    val keyId: String,
    val ownerId: String,
    val encryptedCode: String,
    val encryptedCodeData: String,
    val validityUnix: Long
) {

    fun isStillValid(): Boolean {
        val now = nowUTC().epochSeconds
        return validityUnix <= 0 || validityUnix >= now
    }

    fun toPostJson(): Map<String, Any> {
        return buildMap {
            put("keyId", keyId)
            put("ownerId", ownerId)
            put("encryptedCode", encryptedCode)
            put("encryptedCodeData", encryptedCodeData)
            put("validityUnix", validityUnix)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): PersonalCodeRaw {
            return PersonalCodeRaw(
                id = map["\$id"] as String,
                keyId = map["keyId"] as String,
                ownerId = map["ownerId"] as String,
                encryptedCode = map["encryptedCode"] as String,
                encryptedCodeData = map["encryptedCodeData"] as String,
                validityUnix = (map["validityUnix"] as Number).toLong()
            )
        }
    }
}
