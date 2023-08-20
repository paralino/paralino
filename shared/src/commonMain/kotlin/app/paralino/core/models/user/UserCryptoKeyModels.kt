package app.paralino.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class AsymmetricCryptoKeyData constructor(
    val privateKey: String,
    val publicKey: String,
    val version: Long
)

@Serializable
data class SimpleKeyPairData constructor(
    val privateKey: String,
    val publicKey: String
)

@Serializable
data class SymmetricCryptoKeyData constructor(
    val encryptedKey: String,
    val version: Long
)

data class SessionCryptoKeyData constructor(
    val encryptedKey: String,
    val sessionKey: String,
)