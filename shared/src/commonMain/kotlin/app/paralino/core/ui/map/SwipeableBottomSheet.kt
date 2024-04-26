package app.paralino.core.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ResistanceConfig
import androidx.compose.material.SwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@ExperimentalMaterialApi
@Composable
fun SwipeableBottomSheet(
    swipeableState: SwipeableState<BottomSheetPosition>,
    createAnchors: (screenHeightPixels: Float) -> Map<Float, BottomSheetPosition> = {
        BottomSheetPosition.swipeAnchors(
            screenHeightPixels = it,
            collapsedAnchorBottomPaddingPixels = 0f
        )
    },
    screenContent: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints {
        val constraintsScope = this
        val screenHeightPixels = with(LocalDensity.current) {
            constraintsScope.maxHeight.toPx()
        }

        val connection = remember {
            object : NestedScrollConnection {

                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    return if (delta < 0) {
                        swipeableState.performDrag(delta).toOffset()
                    } else {
                        Offset.Zero
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    return swipeableState.performDrag(delta).toOffset()
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return if (available.y < 0 && scrollState.value == 0) {
                        swipeableState.performFling(available.y)
                        available
                    } else {
                        Velocity.Zero
                    }
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    swipeableState.performFling(velocity = available.y)
                    return super.onPostFling(consumed, available)
                }

                private fun Float.toOffset() = Offset(0f, this)
            }
        }

        screenContent()
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = 0,
                        y = swipeableState.offset.value.roundToInt()
                    )
                }
                .swipeable(
                    state = swipeableState,
                    orientation = Orientation.Vertical,
                    anchors = createAnchors(screenHeightPixels),
                    resistance = ResistanceConfig(screenHeightPixels.times(0.2f), 10f, 20f),
                )
                .nestedScroll(connection)

        ) {
            Column(
                Modifier
                    .fillMaxHeight(0.9f)
                    .background(
                        color = MaterialTheme.colors.background,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                body()
            }
        }
    }
}

enum class BottomSheetPosition constructor(val screenHeightPercent: Float) {
    EXPANDED(screenHeightPercent = 0.85f),
    HALF_EXPANDED(screenHeightPercent = 0.4f),
    COLLAPSED(screenHeightPercent = 0.12f),
    HIDDEN(screenHeightPercent = 0f);

    val remainingScreenHeightPercent: Float get() = 1f - screenHeightPercent

    companion object {
        fun swipeAnchors(
            screenHeightPixels: Float,
            collapsedAnchorBottomPaddingPixels: Float
        ): Map<Float, BottomSheetPosition> {
            return entries.associateBy {
                screenHeightPixels
                    .times(it.remainingScreenHeightPercent)
                    .minus(if (it == COLLAPSED) collapsedAnchorBottomPaddingPixels else 0f)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
fun SwipeableState<BottomSheetPosition>.isFullyExpanded(): Boolean {
    return currentValue == BottomSheetPosition.EXPANDED
}

@OptIn(ExperimentalMaterialApi::class)
fun SwipeableState<BottomSheetPosition>.isCollapsed(): Boolean {
    return currentValue == BottomSheetPosition.COLLAPSED
}