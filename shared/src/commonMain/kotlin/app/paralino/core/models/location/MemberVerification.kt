package app.paralino.core.models.location

@kotlinx.serialization.Serializable
data class MemberVerification(
    val groupOwnerVerificationSecret: String,
    val memberVerificationSecret: String
)
