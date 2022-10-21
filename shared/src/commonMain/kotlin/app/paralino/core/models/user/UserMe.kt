package app.paralino.core.models.user

import app.paralino.core.models.base.ImageModel
import kotlinx.serialization.Serializable

@Serializable
data class UserMe constructor(
    val id: String = "",

    val email: String = "",
    val registration: String = "",
    val emailVerification: Boolean = false,

    val data: UserMeData? = null
) {

    val name: String get() = data?.userName ?: ""

    val isGuestUser: Boolean get() = email.endsWith(guestUserEmail)
    val hasValidData: Boolean get() = !data?.userName.isNullOrBlank() && data?.userImage?.takeIfValid() != null
    val needsEmailVerification: Boolean get() = !emailVerification && !isGuestUser

    companion object {
        const val guestUserEmail = "@guest.paralino.app"
    }

}

@Serializable
data class UserMeData constructor(
    var userImage: ImageModel? = null,
    val userName: String = "",
    val pushToken: String = ""
)