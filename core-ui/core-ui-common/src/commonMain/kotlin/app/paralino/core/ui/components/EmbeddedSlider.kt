package app.paralino.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
private fun rememberAnchoredDraggableState(
    key: Any,
    initialValue: Int,
    anchors: DraggableAnchors<Int>
): AnchoredDraggableState<Int> {
    return remember(key) {
        AnchoredDraggableState(
            initialValue = initialValue,
            anchors = anchors
        )
    }
}

@Composable
fun EmbeddedSlider(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    size: EmbeddedSliderSize = EmbeddedSliderSize(),
    backgroundColor: Color = MaterialTheme.colors.surface,
    swipeEnabled: Boolean = true,
    progress: Int,
    steps: Int,
    infoContent: @Composable () -> Unit,
    onProgressChange: ((Int) -> Unit)? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .defaultMinSize(
                minHeight = size.minHeight
            )
            .clip(shape)
            .background(backgroundColor)
    ) {
        val (viewWidthPx, contentWidthPx) = with(LocalDensity.current) {
            maxWidth.toPx() to size.minStepWidth.toPx()
        }

        val stepPx = (viewWidthPx - contentWidthPx) / steps.minus(1)
        val anchors = remember(steps, stepPx, contentWidthPx) {
            DraggableAnchors {
                repeat(steps) {
                    it at (it * stepPx + contentWidthPx)
                }
            }
        }
        
        val anchoredDraggableState = rememberAnchoredDraggableState(
            key = steps,
            initialValue = progress,
            anchors = anchors
        )

        // Handle progress changes from external source
        LaunchedEffect(progress) {
            snapshotFlow { progress }
                .distinctUntilChanged()
                .collectLatest {
                    delay(300)
                    if (anchoredDraggableState.currentValue != it && anchoredDraggableState.targetValue != it) {
                        anchoredDraggableState.animateTo(progress)
                    }
                }
        }

        // Handle drag completion and notify parent
        LaunchedEffect(steps) {
            snapshotFlow { anchoredDraggableState.targetValue }
                .distinctUntilChanged()
                .collectLatest { onProgressChange?.invoke(it) }
        }

        Box(
            modifier = Modifier
                .defaultMinSize(
                    minHeight = size.minHeight
                )
                .fillMaxSize()
                .anchoredDraggable(
                    state = anchoredDraggableState,
                    orientation = Orientation.Horizontal,
                    enabled = swipeEnabled,
                    overscrollEffect = rememberOverscrollEffect()
                )
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minHeight = size.minHeight
                    )
                    .fillMaxHeight()
                    .width(
                        with(LocalDensity.current) {
                            anchoredDraggableState.offset.toDp()
                        }
                    )
                    .clip(shape)
                    .background(MaterialTheme.colors.primary),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = CoreAppIcons.ChevronLeft.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.lightSurface
                )
                Image(
                    painter = CoreAppIcons.DragIndicator.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .width(10.dp)
                        .height(16.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(AppColors.lightSurface)
                )
                Icon(
                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.lightSurface
                )
            }
        }

        infoContent()
    }
}

@Composable
fun EmbeddedSlider(
    modifier: Modifier = Modifier,
    title: String? = null,
    value: String? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    size: EmbeddedSliderSize = EmbeddedSliderSize(),
    swipeEnabled: Boolean = true,
    progress: Int,
    steps: Int,
    onProgressChange: ((Int) -> Unit)? = null,
) {
    EmbeddedSlider(
        modifier = modifier,
        shape = shape,
        size = size,
        swipeEnabled = swipeEnabled,
        progress = progress,
        steps = steps,
        infoContent = {
            if (!title.isNullOrBlank() || !value.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.surface.copy(alpha = 0.9f))
                        .animateContentSize()
                        .padding(vertical = 3.dp, horizontal = 6.dp),

                    ) {
                    if (!title.isNullOrBlank()) {
                        Text(
                            modifier = Modifier.align(Alignment.TopStart),
                            text = title,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface
                        )
                    }

                    if (!value.isNullOrBlank()) {
                        Text(
                            modifier = Modifier.align(Alignment.BottomStart),
                            text = value,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
        },
        onProgressChange = onProgressChange
    )
}

data class EmbeddedSliderSize(
    val minWidth: Dp = 200.dp,
    val minHeight: Dp = 64.dp,
    val minStepWidth: Dp = 42.dp
)