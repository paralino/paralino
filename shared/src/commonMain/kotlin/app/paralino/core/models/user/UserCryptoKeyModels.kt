package app.paralino.core.models.user

import kotlinx.serialization.Serializable

@Serializable
data class OldMasterKeys(
    val keys: List<SymmetricCryptoKeyData>
)

@Serializable
data class SymmetricCryptoKeyData constructor(
    val key: String,
    val version: Long
)

data class SessionCryptoKeyData constructor(
    val encryptedKey: String,
    val sessionKey: String,
)