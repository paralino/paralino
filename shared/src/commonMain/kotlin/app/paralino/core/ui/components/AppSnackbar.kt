package app.paralino.core.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.utils.extensions.annotatedBold
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppSnackbar(
    snackbarHostState: SnackbarHostState
) {

    if (snackbarHostState.currentSnackbarData == null) {
        return
    }
    var size by remember { mutableStateOf(Size.Zero) }
    val swipeableState = rememberSwipeableState(SwipeDirection.Initial)
    val width = remember(size) {
        if (size.width == 0f) {
            1f
        } else {
            size.width
        }
    }
    if (swipeableState.isAnimationRunning) {
        DisposableEffect(Unit) {
            onDispose {
                when (swipeableState.currentValue) {
                    SwipeDirection.Right,
                    SwipeDirection.Left -> {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }

                    else -> {
                        return@onDispose
                    }
                }
            }
        }
    }

    val offset = with(LocalDensity.current) {
        swipeableState.offset.value.toDp()
    }

    SnackbarHost(
        modifier = Modifier
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
            .swipeable(
                state = swipeableState,
                anchors = mapOf(
                    -width to SwipeDirection.Left,
                    0f to SwipeDirection.Initial,
                    width to SwipeDirection.Right,
                ),
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            ),
        hostState = snackbarHostState,
        snackbar = { snackbarData ->
            val actionLabel = snackbarData.actionLabel
            val actionComposable: (@Composable () -> Unit)? = if (actionLabel != null) {
                @Composable {
                    TextButton(
                        modifier = Modifier.padding(end = 4.dp),
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

            LaunchedEffect(snackbarData) {
                swipeableState.animateTo(SwipeDirection.Initial)
            }

            Snackbar(
                modifier = Modifier
                    .padding(20.dp)
                    .padding(bottom = 20.dp)
                    .alpha(((width - offset.value).absoluteValue / width))
                    .offset(x = offset),
                content = {
                    Text(
                        modifier = Modifier
                            .padding(bottom = if (actionLabel != null) 8.dp else 0.dp)
                            .padding(
                                horizontal = 4.dp,
                                vertical = if (actionLabel != null) 0.dp else 16.dp
                            ),
                        text = snackbarData.message.annotatedBold(),
                        textAlign = TextAlign.Start,
                    )
                },
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

data class AppSnackbarResult constructor(
    val data: AppSnackbarData,
    val result: SnackbarResult
)

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

enum class SwipeDirection {
    Left,
    Initial,
    Right,
}
