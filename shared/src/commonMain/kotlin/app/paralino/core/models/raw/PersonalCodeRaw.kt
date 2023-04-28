package app.paralino.core.models.raw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalCodeRaw constructor(
    @SerialName("\$id")
    val id: String,
    val keyId: String,
    val ownerUserId: String,
    val encryptedCode: String,
    val encryptedCodeKeyData: String?,
    val encryptedCodeUserData: String?,
    val integrity: String?,
    val encryptedUserKeys: String?
) {

    fun isStillValid(): Boolean {
        return arrayOf(encryptedCodeKeyData, encryptedCodeUserData, encryptedUserKeys, integrity).none { it.isNullOrBlank() }
    }

    companion object {
        fun from(map: Map<String, Any>): PersonalCodeRaw {
            return PersonalCodeRaw(
                id = map["\$id"] as String,
                keyId = map["keyId"] as String,
                ownerUserId = map["ownerUserId"] as String,
                encryptedCode = map["encryptedCode"] as String,
                encryptedCodeKeyData = map["encryptedCodeKeyData"] as? String?,
                encryptedCodeUserData = map["encryptedCodeUserData"] as? String?,
                encryptedUserKeys = map["encryptedUserKeys"] as? String?,
                integrity = map["integrity"] as? String?
            )
        }
    }
}
