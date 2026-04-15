package app.paralino.core.presentation.common

data class MemberActionItemState(
    val type: MemberActionType,
    val isLoading: Boolean = false,
    val isEnabled: Boolean = true,
)

enum class MemberActionType {
    DELETE_MEMBER, DELETE_LOCATION, BLOCK_USER, UNBLOCK_USER;
}