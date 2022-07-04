package app.paralino.mobile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.paralino.mobile.utils.extensions.annotatedBold

@Composable
fun AppSnackbar(
    snackbarHostState: SnackbarHostState
) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { snackbarData ->
            val actionLabel = snackbarData.actionLabel
            val actionComposable: (@Composable () -> Unit)? = if (actionLabel != null) {
                @Composable {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        onClick = { snackbarData.performAction() },
                        content = { Text(actionLabel) }
                    )
                }
            } else {
                null
            }

            Snackbar(
                modifier = Modifier.padding(12.dp),
                content = { Text(snackbarData.message.annotatedBold()) },
                action = actionComposable,
                actionOnNewLine = false,
                shape = MaterialTheme.shapes.medium,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                elevation = 1.dp
            )
        }
    )
}

fun SnackbarResult.onAction(action: () -> Unit): SnackbarResult {
    if (this == SnackbarResult.ActionPerformed)
        action()
    return this
}

fun SnackbarResult.onDismiss(action: () -> Unit): SnackbarResult {
    if (this == SnackbarResult.Dismissed)
        action()
    return this
}

data class AppSnackbarData constructor(
    val text: String,
    val actionText: String? = null,
    val duration: SnackbarDuration = if (actionText.isNullOrBlank()) SnackbarDuration.Short else SnackbarDuration.Forever,
    val id: String = text,
)

enum class SnackbarDuration {
    Short,
    Long,
    Forever
}