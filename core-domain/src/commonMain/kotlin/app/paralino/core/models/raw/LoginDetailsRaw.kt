package app.paralino.core.models.raw

import app.paralino.core.crypto.KDFAlg
import kotlinx.serialization.Serializable

@Serializable
data class LoginDetailsRaw(
    val loginSalt: String,
    val loginAlg: KDFAlg
)
