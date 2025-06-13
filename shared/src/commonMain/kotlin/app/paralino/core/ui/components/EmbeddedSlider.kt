package app.paralino.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EmbeddedSlider(
    modifier: Modifier = Modifier,
    title: String? = null,
    value: String? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    size: EmbeddedSliderSize = EmbeddedSliderSize(),
    progress: Int,
    steps: Int,
    onProgressChange: ((Int) -> Unit)? = null,
) {

    val sourceColor = MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))

    BoxWithConstraints(
        modifier = modifier
            .defaultMinSize(
                minHeight = size.minHeight
            )
            .clip(shape)
            .background(backgroundColor)
    ) {
        val swipeableState = rememberSwipeableState(progress)

        LaunchedEffect(Unit) {
            snapshotFlow { swipeableState.targetValue }
                .distinctUntilChanged()
                .collectLatest { onProgressChange?.invoke(it) }
        }

        val (viewWidthPx, contentWidthPx) = with(LocalDensity.current) {
            maxWidth.toPx() to size.minStepWidth.toPx()
        }

        val stepPx = (viewWidthPx - contentWidthPx) / steps.minus(1)
        val anchors = remember(steps, stepPx, contentWidthPx) {
            mutableMapOf<Float, Int>().apply {
                repeat(steps) {
                    this.put(it * stepPx + contentWidthPx, it)
                }
            }
        }

        Box(
            modifier = Modifier
                .defaultMinSize(
                    minHeight = size.minHeight
                )
                .fillMaxSize()
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    orientation = Orientation.Horizontal
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
                            swipeableState.offset.value.toDp()
                        }
                    )
                    .clip(RoundedCornerShape(12.dp))
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

        if (!title.isNullOrBlank() || !value.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .animateContentSize()
                    .padding(vertical = 3.dp, horizontal = 6.dp),

            ) {
                if (!title.isNullOrBlank()) {
                    Text(
                        modifier = Modifier.align(Alignment.TopStart),
                        text = title,
                        style = MaterialTheme.typography.caption,
                        color = contentColor
                    )
                }

                if (!value.isNullOrBlank()) {
                    Text(
                        modifier = Modifier.align(Alignment.BottomStart),
                        text = value,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = contentColor
                    )
                }
            }
        }
    }
}

data class EmbeddedSliderSize(
    val minWidth: Dp = 200.dp,
    val minHeight: Dp = 64.dp,
    val minStepWidth: Dp = 42.dp
)