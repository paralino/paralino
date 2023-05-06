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

    @Contextual
    val codeData: CryptoResource<PersonalCodeUserData>,
) {

    fun isStillValid(): Boolean {
        return codeData is CryptoResource.Ready
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
    val signedOwnerKxPublicKey: String,
    val code: String
) {
    val formattedCode get() = when {
        code.length <= 5 -> code
        else -> code.chunked(code.length / 2).joinToString(separator = "-")
    }
}

