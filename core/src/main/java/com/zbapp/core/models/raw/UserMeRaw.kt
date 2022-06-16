package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class UserMeRaw constructor(
    val id: String = "",

    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,
    val data: UserMeDataRaw? = null,
)

@Serializable
data class UserMeDataRaw constructor(
    var userImage: String? = null,
    var userName: String? = null,
) {
    fun toPostJson(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            userImage?.let { put("userImage", it) }
            userName?.let { put("userName", it) }
        }
    }

    companion object {
        fun parseData(prefs: Map<String, Any>): UserMeDataRaw? {
            return UserMeDataRaw(
                userImage = prefs["userImage"] as? String,
                userName = prefs["userName"] as? String
            )
        }
    }
}
