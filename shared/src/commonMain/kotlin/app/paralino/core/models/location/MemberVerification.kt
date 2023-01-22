package app.paralino.core.models.location

@kotlinx.serialization.Serializable
data class MemberVerification(
    val groupOwnerSecret: String,
    val memberSecret: String
)
