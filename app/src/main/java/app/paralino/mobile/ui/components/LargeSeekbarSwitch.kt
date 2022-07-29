package app.paralino.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LargeSeekbarSwitch(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String? = null,
    focusDescription: Boolean = false,
    state: LargeSeekbarSwitchState.SeekbarSwitchState,
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
                minHeight = LargeSwitchDefaults.minHeight
            )
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .run {
                if (state.switchState != null)
                    this.clickable { onStateChange?.invoke(!state.switchState.checkState) }
                else this
            }
    ) {

        if (state.seekbarState != null && state.switchState?.checkState != false) {
            val seekState = state.seekbarState
            val swipeableState = rememberSwipeableState(seekState.seekbarProgress)
            swipeableState.targetValue.takeIf { it != seekState.seekbarProgress }
                ?.let { onProgressChange?.invoke(it) }
            val stepPx = this.maxWidth.value / seekState.stepsCount
            val anchors = mutableMapOf<Float, Int>().apply {
                seekState.seekbarRange.forEachIndexed { index, i ->
                    this.put(index * stepPx + stepPx, i)
                }
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(
                        minHeight = LargeSwitchDefaults.minHeight
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

                Box(
                    modifier = Modifier
                        .defaultMinSize(
                            minHeight = LargeSwitchDefaults.minHeight,
                            minWidth = stepPx.dp
                        )
                        .fillMaxHeight()
                        .width(swipeableState.offset.value.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(imageVector = Icons.Rounded.DragIndicator,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = backgroundColor)
                }
            }
        }

        ConstraintLayout(
            modifier = Modifier
                .defaultMinSize(
                    minHeight = LargeSwitchDefaults.minHeight,
                    minWidth = LargeSwitchDefaults.minWidth
                )
                .fillMaxSize()
                .padding(vertical = 16.dp),
        ) {

            val (iconRef, textRef, indicatorRef) = createRefs()

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .constrainAs(iconRef) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                    }
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

            Column(
                modifier = Modifier
                    .constrainAs(textRef) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                    }
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .animateContentSize()
                    .padding(vertical = 2.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.caption,
                    color = contentColor
                )
                if (!description.isNullOrBlank()) {
                    if (focusDescription) {
                        Text(
                            text = description,
                            maxLines = 1,
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    } else {
                        Text(
                            text = description,
                            maxLines = 1,
                            style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                            color = contentColor.copy(alpha = ContentAlpha.disabled)
                        )
                    }
                }
            }

            Box(modifier = Modifier
                .constrainAs(indicatorRef) {
                    end.linkTo(parent.end, margin = 16.dp)
                    top.linkTo(iconRef.top)
                    bottom.linkTo(iconRef.bottom)
                }
                .size(28.dp)
            ) {
                if (state.switchState != null) {
                    if (state.switchState.checkState) {
                        Icon(modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colors.surface, shape = CircleShape)
                            .padding(4.dp),
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary)
                    } else {
                        Icon(modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor, shape = CircleShape)
                            .padding(4.dp),
                            imageVector = Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = ContentAlpha.disabled))
                    }
                }
            }

        }

    }
}

object LargeSwitchDefaults {
    val minWidth = 160.dp
    val minHeight = 120.dp
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
    data class Switch(val checkState: Boolean)

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