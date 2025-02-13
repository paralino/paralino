package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.extensions.annotatedBoldCompose
import kotlinx.coroutines.delay
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
    val density = LocalDensity.current

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

    val offset = with(density) {
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
                    FilledButton(
                        style = ButtonStyle.BIG,
                        onClick = { snackbarData.performAction() },
                        content = {
                            Text(text = actionLabel)
                        }
                    )
                }
            } else {
                null
            }

            LaunchedEffect(snackbarData) {
                swipeableState.animateTo(SwipeDirection.Initial)
                when (snackbarData.duration) {
                    androidx.compose.material.SnackbarDuration.Short -> {
                        delay(3600)
                        if (snackbarHostState.currentSnackbarData != null) {
                            swipeableState.animateTo(SwipeDirection.Right)
                        }
                    }

                    androidx.compose.material.SnackbarDuration.Long -> {
                        delay(9500)
                        if (snackbarHostState.currentSnackbarData != null) {
                            swipeableState.animateTo(SwipeDirection.Right)
                        }
                    }

                    androidx.compose.material.SnackbarDuration.Indefinite -> {}
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(((width - offset.value.absoluteValue * 6) / width))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colors.background,
                                    MaterialTheme.colors.background
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = offset)
                        .padding(horizontal = 10.dp)
                        .padding(top = 120.dp, bottom = 30.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colors.surface)
                        .padding(20.dp),
                ) {
                    Text(
                        modifier = Modifier
                            .padding(4.dp),
                        text = snackbarData.message.annotatedBoldCompose(),
                        textAlign = TextAlign.Start,
                    )
                    actionComposable?.let {
                        Spacer(modifier = Modifier.size(20.dp))
                        it()
                    }
                }
            }
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
