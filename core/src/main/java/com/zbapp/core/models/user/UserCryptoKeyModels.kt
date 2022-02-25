package com.zbapp.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class AsymmetricCryptoKeyData(
    val privateKey: String,
    val publicKey: String,
    val version: Int
)

@Serializable
data class SymmetricCryptoKeyData(
    val encryptedKey: String,
    val version: Int
)