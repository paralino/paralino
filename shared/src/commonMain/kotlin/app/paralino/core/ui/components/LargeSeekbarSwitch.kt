package app.paralino.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
fun LargeSeekbarSwitch(
    modifier: Modifier = Modifier,
    icon: Painter?,
    title: String,
    description: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.body2,
    focusDescription: Boolean = false,
    state: LargeSeekbarSwitchState.SeekbarSwitchState,
    size: LargeSwitchSize = LargeSwitchSize(),
    onProgressChange: ((Int) -> Unit)? = null,
    onStateChange: ((Boolean) -> Unit)? = null,
) {

    val sourceColor =
        if (state.switchState?.checkState == true && state.seekbarState == null) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))

    BoxWithConstraints(
        modifier = modifier
            .defaultMinSize(
                minHeight = size.minHeight
            )
            .run {
                if (state.switchState != null)
                    this.clickable { onStateChange?.invoke(!state.switchState.checkState) }
                else this
            }
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.DarkGray)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {

        if (state.seekbarState != null && state.switchState?.checkState != false) {
            val seekState = state.seekbarState
            val swipeableState = rememberSwipeableState(seekState.seekbarProgress)

            LaunchedEffect(Unit) {
                snapshotFlow { swipeableState.targetValue }
                    .distinctUntilChanged()
                    .collectLatest { onProgressChange?.invoke(it) }
            }

            val (viewWidthPx, contentWidthPx) = with(LocalDensity.current) {
                maxWidth.toPx() to size.minStepWidth.toPx()
            }

            val stepPx = (viewWidthPx - contentWidthPx) / seekState.stepsCount.minus(1)

            val anchors = mutableMapOf<Float, Int>().apply {
                seekState.seekbarRange.forEachIndexed { index, i ->
                    this.put(index * stepPx + contentWidthPx, i)
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
                        thresholds = { _, _ -> FractionalThreshold(0.3f) },
                        orientation = Orientation.Horizontal,
                        resistance = null
                    )
            ) {

                Row(
                    modifier = Modifier
                        .defaultMinSize(
                            minHeight = size.minHeight,
                            minWidth = with(LocalDensity.current) {
                                contentWidthPx.toDp()
                            }
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
        }

        val hasTopContent = remember(icon, state.switchState) {
            icon != null || state.switchState != null
        }

        Column(
            modifier = Modifier
                .defaultMinSize(
                    minHeight = size.minHeight,
                    minWidth = size.minWidth
                )
                .fillMaxSize()
                .padding(
                    top = if (hasTopContent) 16.dp else 6.dp,
                    bottom = if (hasTopContent) 14.dp else 6.dp
                ),
        ) {
            if (hasTopContent) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(36.dp)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(backgroundColor)
                                .padding(8.dp),
                            tint = contentColor
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                    ) {
                        if (state.switchState != null) {
                            if (state.switchState.checkState) {
                                Icon(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colors.onPrimary,
                                            shape = CircleShape
                                        )
                                        .padding(1.dp),
                                    painter = state.switchState.checkedIconRes.asPainterResource(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary
                                )
                            } else {
                                Icon(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(backgroundColor, shape = CircleShape)
                                        .padding(2.dp),
                                    painter = state.switchState.unCheckedIconRes.asPainterResource(),
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = ContentAlpha.disabled)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = if (hasTopContent) 12.dp else 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .animateContentSize()
                    .padding(vertical = 2.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = contentColor
                )
                if (!description.isNullOrBlank()) {
                    if (focusDescription) {
                        Text(
                            text = description,
                            maxLines = 3,
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    } else {
                        Text(
                            text = description,
                            maxLines = 3,
                            style = MaterialTheme.typography.caption,
                            color = contentColor.copy(alpha = ContentAlpha.medium)
                        )
                    }
                }
            }
        }
    }
}

data class LargeSwitchSize(
    val minWidth: Dp = 160.dp,
    val minHeight: Dp = 120.dp,
    val minStepWidth: Dp = 42.dp
)

object LargeSwitchDefaults {
    val size = LargeSwitchSize()
    val minWidth = size.minWidth
    val minHeight = size.minHeight
}

@Composable
fun rememberSeekbarState(
    state: LargeSeekbarSwitchState.SeekbarSwitchState,
) = remember {
    mutableStateOf(state)
}

@Composable
fun rememberSeekbarState(
    key: Any? = null,
    state: LargeSeekbarSwitchState.SeekbarSwitchState,
) = remember(key) {
    mutableStateOf(state)
}

object LargeSeekbarSwitchState {
    data class Switch(
        val checkState: Boolean,
        val checkedIconRes: String = CoreAppIcons.CheckCircle,
        val unCheckedIconRes: String = CoreAppIcons.RadioButtonUnchecked
    )

    data class Seekbar(
        val seekbarRange: IntRange,
        val seekbarProgress: Int,
    ) {
        val stepsCount = seekbarRange.count()
        val rangeValue: Int = seekbarRange.last - seekbarRange.first
        val progress: Float = (seekbarProgress - seekbarRange.first).toFloat() / rangeValue
    }

    data class SeekbarSwitchState(
        val switchState: Switch?,
        val seekbarState: Seekbar?,
    )
}