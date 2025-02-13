package app.paralino.core.models.raw

import app.paralino.core.models.user.UserMe.Companion.guestUserEmail
import app.paralino.core.utils.extensions.isoWithOffsetToInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserMeRaw constructor(
    val id: String = "",

    val email: String = "",
    val registration: String = "",
    val emailVerification: Boolean = false,
    val passwordUpdate: String? = null,
    val data: UserMeDataRaw? = null,
) {
    val isGuestUser: Boolean get() = email.endsWith(guestUserEmail)
    val passwordUpdateTime: Instant? get() {
        return passwordUpdate?.isoWithOffsetToInstant()
    }
}

@Serializable
data class UserMeDataRaw constructor(
    var userImage: String?,
    var userName: String?,
    var userKeyVersion: Long?
) {
    fun toPostJson(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            userImage?.let { put("userImage", it) }
            userName?.let { put("userName", it) }
            userKeyVersion?.let { put("userKeyVersion", it) }
        }
    }

    companion object {
        fun parseData(prefs: Map<String, Any>): UserMeDataRaw {
            return UserMeDataRaw(
                userImage = prefs["userImage"] as? String,
                userName = prefs["userName"] as? String,
                userKeyVersion = (prefs["userKeyVersion"] as? Number?)?.toLong(),
            )
        }
    }
}
