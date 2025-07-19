package app.paralino.core.ui.map

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.current
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterialApi
@Composable
fun SwipeableBottomSheet(
    swipeableState: SwipeableSheetState,
    screenContent: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    BoxWithConstraints {
        val constraintsScope = this
        val screenHeightPixels = with(LocalDensity.current) {
            constraintsScope.maxHeight.toPx()
        }

        swipeableState.createAnchors(screenHeightPixels)

        val connection = rememberBottomSheetNestedScrollConnection(
            swipeableState = swipeableState,
            screenHeightPixels = screenHeightPixels
        )

        screenContent()
        Box(
            modifier = Modifier
                .nestedScroll(connection)
                .offset {
                    IntOffset(
                        x = 0,
                        y = swipeableState.state.requireOffset().roundToInt()
                    )
                }
                .anchoredDraggable(
                    state = swipeableState.state,
                    orientation = Orientation.Vertical,
                    interactionSource = remember { MutableInteractionSource() },
                    overscrollEffect = ScrollableDefaults.overscrollEffect()
                )

        ) {
            Column(
                Modifier
                    .fillMaxHeight(swipeableState.maxSheetContentSizePercent)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberBottomSheetNestedScrollConnection(
    swipeableState: SwipeableSheetState,
    screenHeightPixels: Float,
): NestedScrollConnection {
    return remember(screenHeightPixels) {
        object : NestedScrollConnection {

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                return if (delta < 0) {
                    swipeableState.state.dispatchRawDelta(delta).toOffset()
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
                val calculatedPosition =
                    swipeableState.state.anchors.closestAnchor(swipeableState.offset + delta)
                if (calculatedPosition != null && calculatedPosition < BottomSheetPosition.HIDDEN) {
                    return swipeableState.state.dispatchRawDelta(delta).toOffset()
                } else {
                    return Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (
                    available.y < 0 &&
                    !swipeableState.mapContentListState.isScrollInProgress &&
                    !swipeableState.mapContentListState.canScrollBackward &&
                    !swipeableState.isFullyExpanded()
                ) {
                    swipeableState.settle(available.y)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                swipeableState.settle(velocity = available.y)
                return super.onPostFling(consumed, available)
            }

            private fun Float.toOffset() = Offset(0f, this)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
class SwipeableSheetState internal constructor(
    internal val state: AnchoredDraggableState<BottomSheetPosition>,
    val mapContentListState: LazyListState,
    private val collapsedAnchorBottomPaddingPixels: Float = 0f,
    private val positionalThreshold: (totalDistance: Float) -> Float,
    private val velocityThreshold: () -> Float,
    private val confirmValueChange: (BottomSheetPosition) -> Boolean
) {

    val maxSheetContentSizePercent = 0.95f
    private var anchors: Map<BottomSheetPosition, Float> = emptyMap()
        private set

    var screenHeightPixels: Float = 0f
        private set

    val progress: Float
        get() {
            val offset = state.offset.takeIf { it.isFinite() } ?: return 1f
            val topAnchor = state.anchors.closestAnchor(offset, searchUpwards = true) ?: return 1f
            val bottomAnchor =
                state.anchors.closestAnchor(offset, searchUpwards = false) ?: return 1f

            val topPosition =
                state.anchors.positionOf(topAnchor).takeIf { it.isFinite() } ?: return 1f
            val bottomPosition =
                state.anchors.positionOf(bottomAnchor).takeIf { it.isFinite() } ?: return 1f

            if (topPosition == bottomPosition) return 1f

            return (offset - bottomPosition) / (topPosition - bottomPosition)
        }

    val offset: Float get() = state.offset.let { if (it.isFinite()) it else 0f }
    val currentValue: BottomSheetPosition get() = state.targetValue
    val settledValue: BottomSheetPosition get() = state.settledValue
    val isPastCollapsed: Boolean
        get() {
            return offset > state.anchors.positionOf(BottomSheetPosition.COLLAPSED)
        }

    val contentBottomPadding: Dp
        get() {
            val fixedPadding = ((1f - maxSheetContentSizePercent) * screenHeightPixels).dp
            val extraPadding = when (DeviceType.current) {
                DeviceType.Android -> 0.dp
                DeviceType.iOS -> 20.dp
            }
            return fixedPadding + extraPadding
        }

    suspend fun settle(velocity: Float): Float {
        val previousValue = this.currentValue
        val targetValue = computeTarget(
            offset = state.requireOffset(),
            currentValue = previousValue,
            velocity = velocity
        )

//        var targetValue = if (velocity < 0) {
//            previousValue.nextUp()
//        } else if (velocity > 0) {
//            previousValue.nextDown()
//        } else {
//            null
//        }

//        val targetValue = previousValue.computeTarget(anchors, offset, velocity)

        if (confirmValueChange(targetValue)) {
            state.animateToWithDecay(targetValue = targetValue, velocity = velocity)
        } else {
            state.animateToWithDecay(
                targetValue = previousValue.computeTarget(anchors, offset, velocity),
                velocity = velocity
            )
        }

        return velocity
    }

    private fun computeTarget(
        offset: Float,
        currentValue: BottomSheetPosition,
        velocity: Float
    ): BottomSheetPosition {
        val currentAnchors = state.anchors
        val currentAnchorPosition = currentAnchors.positionOf(currentValue)
        val velocityThresholdPx = velocityThreshold()
        return if (currentAnchorPosition == offset || currentAnchorPosition.isNaN()) {
            currentValue
        } else {
            if (abs(velocity) >= abs(velocityThresholdPx)) {
                currentAnchors.closestAnchor(
                    offset,
                    sign(velocity) > 0
                )!!
            } else {
                val neighborAnchor =
                    currentAnchors.closestAnchor(
                        offset,
                        offset - currentAnchorPosition > 0
                    )!!
                val neighborAnchorPosition = currentAnchors.positionOf(neighborAnchor)
                val distance = abs(currentAnchorPosition - neighborAnchorPosition)
                val relativeThreshold = abs(positionalThreshold(distance))
                val relativePosition = abs(currentAnchorPosition - offset)
                if (relativePosition <= relativeThreshold) currentValue else neighborAnchor
            }
        }
    }

    @Composable
    fun createAnchors(screenHeightPixels: Float) {
        val anchors = remember(screenHeightPixels) {
            this.screenHeightPixels = screenHeightPixels
            anchors = BottomSheetPosition.swipeAnchors(
                screenHeightPixels = screenHeightPixels,
                collapsedAnchorBottomPaddingPixels = collapsedAnchorBottomPaddingPixels
            )
            DraggableAnchors {
                anchors.forEach {
                    it.key at it.value
                }
            }
        }

        SideEffect {
            state.updateAnchors(newAnchors = anchors)
        }
    }

    suspend fun animateTo(position: BottomSheetPosition) {
        state.animateTo(position)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberSwipeableSheetState(
    initialValue: BottomSheetPosition = BottomSheetPosition.default,
    mapContentListState: LazyListState = rememberLazyListState(),
    collapsedAnchorBottomPaddingPixels: Float = 0f,
    confirmValueChange: (BottomSheetPosition) -> Boolean
): SwipeableSheetState {
    val density = LocalDensity.current
    val positionalThreshold: (totalDistance: Float) -> Float =
        remember { { with(density) { 56.dp.toPx() } } }
    val velocityThreshold: () -> Float = remember { { with(density) { 1000.dp.toPx() } } }
    return remember {
        SwipeableSheetState(
            state = AnchoredDraggableState(
                initialValue = initialValue,
                snapAnimationSpec = spring(),
                decayAnimationSpec = exponentialDecay(),
                positionalThreshold = positionalThreshold,
                velocityThreshold = velocityThreshold,
                confirmValueChange = confirmValueChange
            ),
            mapContentListState = mapContentListState,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            confirmValueChange = confirmValueChange,
            collapsedAnchorBottomPaddingPixels = collapsedAnchorBottomPaddingPixels
        )
    }
}

enum class BottomSheetPosition constructor(val screenHeightPercent: Float) {
    EXPANDED(
        screenHeightPercent = when (DeviceType.current) {
            DeviceType.Android -> 0.912f
            DeviceType.iOS -> 0.855f
        }
    ),
    HALF_EXPANDED(screenHeightPercent = 0.4f),
    COLLAPSED(screenHeightPercent = 0.12f),
    HIDDEN(screenHeightPercent = 0f);

    val remainingScreenHeightPercent: Float get() = 1f - screenHeightPercent

    fun nextUp(): BottomSheetPosition? {
        return when (this) {
            EXPANDED -> null
            HALF_EXPANDED -> EXPANDED
            COLLAPSED -> HALF_EXPANDED
            HIDDEN -> COLLAPSED
        }
    }

    fun nextDown(): BottomSheetPosition? {
        return when (this) {
            EXPANDED -> HALF_EXPANDED
            HALF_EXPANDED -> COLLAPSED
            COLLAPSED -> null
            HIDDEN -> null
        }
    }

    fun computeTarget(
        anchors: Map<BottomSheetPosition, Float>,
        currentOffset: Float,
        velocity: Float
    ): BottomSheetPosition {
        // Find the current anchor corresponding to the current value
        val currentAnchor = anchors[this] ?: currentOffset

        // Sort anchors and get their list
        val anchorsList = anchors.values.sorted()
        val currentIndex = anchorsList.indexOf(currentAnchor)

        // Define velocity and positional thresholds
        val velocityThresholdPx = 1000f // Adjust this threshold as needed
        val positionalThreshold = 0.5f // 50% of the distance to the next anchor

        val isVelocityAboveThreshold = velocity.absoluteValue > velocityThresholdPx

        val direction = when {
            velocity < 0 -> -1 // Flinging upwards
            velocity > 0 -> 1  // Flinging downwards
            else -> 0
        }

        var targetIndex = currentIndex

        if (isVelocityAboveThreshold) {
            // Move in the direction of the fling
            targetIndex += direction
        } else {
            // Use positional threshold to determine target
            val nextIndex = (currentIndex + 1).coerceAtMost(anchorsList.lastIndex)
            val prevIndex = (currentIndex - 1).coerceAtLeast(0)
            val nextAnchor = anchorsList[nextIndex]
            val prevAnchor = anchorsList[prevIndex]

            val delta = currentOffset - currentAnchor
            val distanceToNext = nextAnchor - currentAnchor
            val distanceToPrev = currentAnchor - prevAnchor

            if (delta > positionalThreshold * distanceToNext) {
                targetIndex = nextIndex
            } else if (delta < -positionalThreshold * distanceToPrev) {
                targetIndex = prevIndex
            }
        }

        // Ensure the target index is within the bounds of EXPANDED and COLLAPSED
        val expandedAnchor = anchors[BottomSheetPosition.EXPANDED]
        val collapsedAnchor = anchors[BottomSheetPosition.COLLAPSED]

        val minIndex = expandedAnchor?.let { anchorsList.indexOf(it) } ?: 0
        val maxIndex = collapsedAnchor?.let { anchorsList.indexOf(it) } ?: anchorsList.lastIndex

        targetIndex = targetIndex.coerceIn(minIndex, maxIndex)

        val targetAnchor = anchorsList[targetIndex]
        val targetValue = anchors.entries.firstOrNull { it.value == targetAnchor }?.key ?: this

        return when (targetValue) {
            EXPANDED -> EXPANDED
            HALF_EXPANDED -> HALF_EXPANDED
            COLLAPSED -> COLLAPSED
            HIDDEN -> COLLAPSED
        }
    }


    companion object {
        val default: BottomSheetPosition = HALF_EXPANDED

        fun swipeAnchors(
            screenHeightPixels: Float,
            collapsedAnchorBottomPaddingPixels: Float
        ): Map<BottomSheetPosition, Float> {
            return entries.associateWith {
                screenHeightPixels
                    .times(it.remainingScreenHeightPercent)
                    .minus(if (it == COLLAPSED) collapsedAnchorBottomPaddingPixels else 0f)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun SwipeableSheetState.isFullyExpanded(): Boolean {
    return state.currentValue == BottomSheetPosition.EXPANDED
}

@OptIn(ExperimentalFoundationApi::class)
fun SwipeableSheetState.isCollapsed(): Boolean {
    return state.currentValue == BottomSheetPosition.COLLAPSED
}