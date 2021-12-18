package com.zbapp.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserMe constructor(
    val id: String = "",

    val email: String = "",
    val registration: Long = 0,
    val emailVerification: Boolean = false,

    val data: UserMeData? = null
) {

    val name: String get() = data?.userName ?: ""
}

@Serializable
data class UserMeData constructor(
    var imageBase64: String? = null,
    val userName: String = "",
    val privateKeyBackup: SymmetricCryptoKeyData? = null,
    val asymmetricKeys: List<AsymmetricCryptoKeyData>? = null
)

@Serializable
data class AsymmetricCryptoKeyData(
    val privateKey: String,
    val publicKey: String,
    val version: Int
)

@Serializable
data class SymmetricCryptoKeyData(
    val key: String,
    val version: Int
)