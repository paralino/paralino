package app.paralino.core.models.raw

import app.paralino.core.utils.extensions.fromUTCtoInstant
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalCodeRaw constructor(
    @SerialName("\$id")
    val id: String,
    val keyId: String,
    val ownerUserId: String,
    val encryptedCode: String,
    val encryptedCodeData: String,
    val verification: String,
    val validityUTC: String
) {

    fun isStillValid(): Boolean {
        val now = nowUTC()
        val time = validityUTC.fromUTCtoInstant() ?: return false
        return time >= now
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun from(map: Map<String, Any>): PersonalCodeRaw {
            return PersonalCodeRaw(
                id = map["\$id"] as String,
                keyId = map["keyId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedCode = map["encryptedCode"] as String,
                encryptedCodeData = map["encryptedCodeData"] as String,
                verification = map["verification"] as String,
                validityUTC = map["validityUTC"] as String
            )
        }
    }
}
