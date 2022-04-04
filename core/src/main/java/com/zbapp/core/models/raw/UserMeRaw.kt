package com.zbapp.core.models.raw

import kotlinx.serialization.Serializable

@Serializable
data class UserMeRaw constructor(
    val id: String = "",

    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,
    val data: UserMeDataRaw? = null
)

@Serializable
data class UserMeDataRaw constructor(
    var userImage: String? = null,
    var userName: String? = null,
    val privateKeyBackup: SymmetricKeyDataRaw? = null,
    val asymmetricKeys: List<AsymmetricKeyDataRaw>? = null
) {
    fun toPostJson(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            userImage?.let { put("userImage", it) }
            userName?.let { put("userName", it) }
            privateKeyBackup?.let { put("privateKeyBackup", it) }
            asymmetricKeys?.takeIf { it.isNotEmpty() }?.let { keys ->
                put("asymmetricKeys", keys)
            }
        }
    }

    companion object {
        fun parseData(prefs: Map<String, Any>): UserMeDataRaw? {
            return UserMeDataRaw(
                userImage = prefs["userImage"] as? String,
                userName = prefs["userName"] as? String,
                privateKeyBackup = prefs["privateKeyBackup"] as? String,
                asymmetricKeys = prefs["asymmetricKeys"] as? List<String>
            )
        }
    }
}

typealias AsymmetricKeyDataRaw = String
typealias SymmetricKeyDataRaw = String
