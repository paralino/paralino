package app.paralino.core.models.user

import app.paralino.core.models.base.ImageModel
import app.paralino.core.utils.extensions.nowUTC
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPersonalCode constructor(
    @SerialName("\$id")
    val id: String = "",
    val validityUnix: Long,
    val data: UserPersonalCodeData
) {

    fun isStillValid(): Boolean {
        val now = nowUTC().epochSeconds
        return validityUnix <= 0 || validityUnix >= now
    }

}

@Serializable
data class UserPersonalCodeData constructor(
    val ownerId: String,
    val ownerName: String,
    val ownerImage: ImageModel?,
    val ownerPublicKey: String,
    val code: String,
) {
    val formattedCode get() = when {
        code.length <= 5 -> code
        else -> code.chunked(code.length / 2).joinToString(separator = "-")
    }
}

