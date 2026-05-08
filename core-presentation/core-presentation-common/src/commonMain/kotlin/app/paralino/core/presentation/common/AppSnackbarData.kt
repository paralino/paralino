package app.paralino.core.presentation.common

import kotlinx.serialization.Serializable

data class AppSnackbarData constructor(
    val payload: AppSnackbarPayload,
    val actionText: String? = null,
    val duration: SnackbarDuration = if (actionText.isNullOrBlank()) SnackbarDuration.Short else SnackbarDuration.Forever,
    val id: String = payload.text,
) {
    constructor(
        text: String,
        actionText: String? = null,
        duration: SnackbarDuration = if (actionText.isNullOrBlank()) {
            SnackbarDuration.Short
        } else {
            SnackbarDuration.Forever
        },
        id: String = text
    ) : this(
        payload = AppSnackbarPayload(
            title = null,
            text = text,
            hasCloseButton = false
        ),
        actionText = actionText,
        duration = duration,
        id = text
    )
}

fun AppSnackbarResultType.onAction(action: () -> Unit): AppSnackbarResultType {
    if (this == AppSnackbarResultType.ActionPerformed)
        action()
    return this
}

fun AppSnackbarResultType.onDismiss(action: () -> Unit): AppSnackbarResultType {
    if (this == AppSnackbarResultType.Dismissed)
        action()
    return this
}

data class AppSnackbarResult constructor(
    val data: AppSnackbarData,
    val result: AppSnackbarResultType
)

enum class AppSnackbarResultType {
    Dismissed,
    ActionPerformed,
}

@Serializable
data class AppSnackbarPayload(
    val text: String,
    val title: String? = null,
    val hasCloseButton: Boolean = false,
//    val iconRes: Int?,
//    val iconTint: Int?
)

enum class SnackbarDuration {
    Short,
    Long,
    Forever
}

enum class SwipeDirection {
    Left,
    Initial,
    Right,
}