package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class UserMeRaw constructor(
    val id: String = "",

    val name: String = "",
    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,
    val data: UserMeDataRaw? = null
)

@Serializable
data class UserMeDataRaw constructor(
    var imageBase64: String? = null,
    val privateKeyBackup: String? = null,
    val asymmetricKeys: List<String>? = null
) {
    fun toPostJson(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            imageBase64?.let { put("imageBase64", it) }
            privateKeyBackup?.let { put("privateKeyBackup", it) }
            asymmetricKeys?.takeIf { it.isNotEmpty() }?.let { keys ->
                put("asymmetricKeys", keys)
            }
        }
    }
}
