package app.paralino.core.models.user

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPersonalCode constructor(
    @SerialName("\$id")
    val id: String = "",
    val code: String,
    val encryptedUserKeys: String?,
    val integrity: String?,

    @Contextual
    val codeData: CryptoResource<UserPersonalCodeData>,
) {

    fun isStillValid(): Boolean {
        return !encryptedUserKeys.isNullOrBlank() && !integrity.isNullOrBlank() && codeData is CryptoResource.Ready
    }

    val formattedCode: String? get() = when {
        !isStillValid() -> null
        code.length <= 5 -> code
        else -> code.chunked(code.length / 2).joinToString(separator = "-")
    }

}

@Serializable
data class UserPersonalCodeData constructor(
    val userData: PersonalCodeUserData,
    val keyData: PersonalCodeKeyData
) {
}

@Serializable
data class PersonalCodeUserData constructor(
    val ownerUserId: String,
    val ownerName: String,
    val ownerImage: ImageModel?,
    val ownerIdentityKey: String,
) {
}

@Serializable
data class PersonalCodeKeyData constructor(
    val ownerExchangeKey: String,
    val code: String,
    val memberVerificationSecret: String
) {
    val formattedCode get() = when {
        code.length <= 5 -> code
        else -> code.chunked(code.length / 2).joinToString(separator = "-")
    }
}

