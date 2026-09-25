package app.paralino.core.models.user

import app.paralino.core.models.CryptoResource
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.utils.extensions.isOlderThan
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Serializable
data class UserMe constructor(
    val id: String = "",

    val email: String = "",
    val registration: String = "",
    val emailVerification: Boolean = false,
    val emailVerificationSkipTime: Instant? = null,
    val passwordUpdate: Instant? = null,

    @Contextual
    val data: CryptoResource<UserMeData> = CryptoResource.NotInitialized
) {

    val name: String get() = data.data?.userName ?: "---"

    val isGuestUser: Boolean get() = email.endsWith(guestUserEmail)
    val hasValidData: Boolean get() = data is CryptoResource.Ready && data.data.userName.isNotBlank() && data.data.userImage?.takeIfValid() != null
    val canVerifyEmail: Boolean get() = !emailVerification && !isGuestUser
    val needsToVerifyEmail: Boolean get() = canVerifyEmail && (emailVerificationSkipTime?.isOlderThan(24.hours) ?: true)

    companion object {
        const val guestUserEmail = "@guest.paralino.app"
    }
}

@Serializable
data class UserMeData constructor(
    val userImage: ImageModel? = null,
    val userName: String = ""
)