package app.paralino.core.models.group

data class PendingGroupJoinWelcome(
    val groupId: String,
    val didShowWelcome: Boolean,
    val createdAtMillis: Long
)
