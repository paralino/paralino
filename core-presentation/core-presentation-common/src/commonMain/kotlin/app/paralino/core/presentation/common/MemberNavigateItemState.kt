package app.paralino.core.presentation.common

data class MemberNavigateItemState(
    val lat: Double,
    val lng: Double,
    val title: String?,
    val isEnabled: Boolean = true
)