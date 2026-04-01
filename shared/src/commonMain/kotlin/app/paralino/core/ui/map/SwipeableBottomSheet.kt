package app.paralino.core.ui.map

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.shadow.Shadow
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
import app.paralino.core.utils.extensions.conditional
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterialApi
@Composable
fun SwipeableBottomSheet(
    enabled: Boolean,
    screenPaddingValues: PaddingValues,
    collapsedInfoHeightPixel: Float,
    collapsedMinimizedInfoHeightPixel: Float,
    swipeableState: SwipeableSheetState,
    screenContent: @Composable () -> Unit,
    body: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val constraintsScope = this
        val screenHeightPixels = with(LocalDensity.current) {
            constraintsScope.maxHeight.toPx()
        }

        val bottomCornerRadiusDp = remember(swipeableState.bottomScreenCornerRadius) { (swipeableState.bottomScreenCornerRadius * 0.9194).dp }

        swipeableState.createAnchors(
            screenHeightPixels = screenHeightPixels,
            collapsedInfoHeightPixel = collapsedInfoHeightPixel,
            collapsedMinimizedInfoHeightPixel = collapsedMinimizedInfoHeightPixel,
            expandedTopBarHeightPixel = with(LocalDensity.current) {
                (screenPaddingValues.calculateTopPadding() + 70.dp).toPx()
            }
        )

        val connection = rememberBottomSheetNestedScrollConnection(
            swipeableState = swipeableState,
            screenHeightPixels = screenHeightPixels
        )

        screenContent()

        val topShapeCorners by derivedStateOf {
            val progress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.EXPANDED
            ).coerceIn(0f, 1f)
            val breakingPoint = BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
            
            when {
                progress <= breakingPoint -> {
                    // From collapsed to half-expanded (0f to 0.4f): 50dp to 64dp
                    val normalizedProgress = progress / breakingPoint // 0f to 1f
                    SwipeableSheetState.topCornerCollapsed + 
                        ((SwipeableSheetState.topCornerHalfExpanded - SwipeableSheetState.topCornerCollapsed) * normalizedProgress)
                }
                else -> {
                    // From half-expanded to expanded (0.4f to 1f): 64dp to 50dp
                    val normalizedProgress = (progress - breakingPoint) / (1f - breakingPoint) // 0f to 1f
                    SwipeableSheetState.topCornerHalfExpanded - 
                        ((SwipeableSheetState.topCornerHalfExpanded - SwipeableSheetState.topCornerExpanded) * normalizedProgress)
                }
            }
        }

        val bottomShapeCorners by derivedStateOf {
            // min value can not be 0, since on android < 12 there is some very weird bug
            // that makes the whole content disappear if corners are different
            // and sheet is manually set to some position
            val progress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.EXPANDED
            ).coerceIn(0.01f, 1f)
            val breakingPoint = BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
            
            when {
                progress <= breakingPoint -> {
                    // From collapsed to half-expanded (0f to 0.4f): 50dp to 40dp/58dp
                    val normalizedProgress = progress / breakingPoint // 0f to 1f
                    SwipeableSheetState.bottomCornerCollapsed + 
                        ((bottomCornerRadiusDp - SwipeableSheetState.bottomCornerCollapsed) * normalizedProgress)
                }
                else -> {
                    // From half-expanded to expanded (0.4f to 1f): 40dp/58dp to 0dp
                    val normalizedProgress = (progress - breakingPoint) / (1f - breakingPoint) // 0f to 1f
                    bottomCornerRadiusDp - ((bottomCornerRadiusDp - SwipeableSheetState.bottomCornerExpanded) * normalizedProgress)
                }
            }
        }

        val extraHorizontalPadding = if (DeviceType.current == DeviceType.iOS) {
            // From collapsed minimized to collapsed
            val collapsedMinimizedProgress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.COLLAPSED_MINIMISED
            ).coerceIn(0f, 1f)
            SwipeableSheetState.sheetHorizontalCollapsedMinimizedPadding
                .times(collapsedMinimizedProgress)
        } else {
            0.dp
        }

        val horizontalPadding by derivedStateOf {
            val progress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.EXPANDED
            ).coerceIn(0f, 1f)
            val breakingPoint = BottomSheetPosition.HALF_EXPANDED.screenHeightPercent

            when {
                progress <= breakingPoint -> {
                    // From collapsed to half-expanded (0f to 0.4f): 16dp to 10dp
                    val normalizedProgress = progress / breakingPoint // 0f to 1f
                    SwipeableSheetState.sheetHorizontalCollapsedPadding
                        .minus(
                            SwipeableSheetState.sheetHorizontalCollapsedPadding
                                .minus(SwipeableSheetState.sheetHorizontalHalfExpandedPadding)
                                .times(normalizedProgress)
                        ) // 16dp - 6dp = 10dp at progress 1f
                        .plus(extraHorizontalPadding)
                }
                else -> {
                    // From half-expanded to expanded (0.4f to 1f): 10dp to 0dp
                    val normalizedProgress = (progress - breakingPoint) / (1f - breakingPoint) // 0f to 1f
                    SwipeableSheetState.sheetHorizontalHalfExpandedPadding
                        .minus(
                            SwipeableSheetState.sheetHorizontalHalfExpandedPadding
                                .times(normalizedProgress)
                        ) // 10dp - 10dp = 0dp at progress 1f
                }
            }
        }

        val extraBottomPadding = if (DeviceType.current == DeviceType.iOS) {
            // From collapsed minimized to collapsed
            val collapsedMinimizedProgress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.COLLAPSED_MINIMISED
            ).coerceIn(0f, 1f)
            SwipeableSheetState.sheetBottomCollapsedMinimizedPadding
                .times(collapsedMinimizedProgress)
        } else {
            0.dp
        }

        val bottomPadding by derivedStateOf {
            val breakingPoint = BottomSheetPosition.HALF_EXPANDED.screenHeightPercent
            val collapsedProgress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED,
                to = BottomSheetPosition.EXPANDED
            ).coerceIn(0f, 1f)
            when {
                collapsedProgress <= breakingPoint -> {
                    // From collapsed to half-expanded (0f to 0.4f): 16dp to 10dp
                    val normalizedProgress = collapsedProgress / breakingPoint // 0f to 1f
                    screenPaddingValues.calculateBottomPadding()
                        .plus(
                            SwipeableSheetState.sheetBottomCollapsedPadding
                                .minus(
                                    SwipeableSheetState.sheetBottomCollapsedPadding
                                        .minus(SwipeableSheetState.sheetBottomHalfExpandedPadding)
                                        .times(normalizedProgress)
                                ) // 16dp - 6dp = 10dp at progress 1f
                        )
                        .plus(extraBottomPadding)
                }
                else -> {
                    // From half-expanded to expanded (0.4f to 1f): 10dp to 0dp
                    val normalizedProgress = (collapsedProgress - breakingPoint) / (1f - breakingPoint) // 0f to 1f
                    screenPaddingValues.calculateBottomPadding()
                        .plus(SwipeableSheetState.sheetBottomHalfExpandedPadding)
                        .times(1f - normalizedProgress)
                }
            }
        }

        val sheetWidth by derivedStateOf {
            val progress = swipeableState.progress(
                from = BottomSheetPosition.COLLAPSED_MINIMISED,
                to = BottomSheetPosition.COLLAPSED
            ).coerceIn(0f, 1f)

            val minWidth = 70.dp
            constraintsScope.maxWidth.times(progress).coerceAtLeast(minWidth)
        }

        val screenShape = RoundedCornerShape(
            topStart = topShapeCorners,
            topEnd = topShapeCorners,
            bottomStart = bottomShapeCorners,
            bottomEnd = bottomShapeCorners
        )

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = bottomPadding)
                    .width(
                        width = sheetWidth
                    )
                    .height(
                        height = with(LocalDensity.current) {
                            (screenHeightPixels - swipeableState.offset.roundToInt())
                                .toDp()
                        }
                            .minus(bottomPadding)
                    )
                    .dropShadow(
                        shape = screenShape,
                        shadow = Shadow(
                            radius = 10.dp,
                            spread = 4.dp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
//                            offset = DpOffset(
//                                x = 0.dp,
//                                // offset doesn't work on iOS if shape corners are different
//                                y = when (DeviceType.current) {
//                                    DeviceType.Android -> 0.dp
//                                    DeviceType.iOS -> 0.dp
//                                }
//                            )
                        )
                    )
                    .clip(screenShape)
                    .background(
                        color = MaterialTheme.colors.background,
                        shape = screenShape
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = bottomPadding)
                    .clip(screenShape)
                    .conditional(enabled) {
                        nestedScroll(connection)
                    }
                    .offset {
                        IntOffset(
                            x = 0,
                            y = swipeableState.state.requireOffset().roundToInt()
                        )
                    }
                    .anchoredDraggable(
                        enabled = enabled,
                        state = swipeableState.state,
                        orientation = Orientation.Vertical,
                        interactionSource = remember { MutableInteractionSource() },
                        overscrollEffect = rememberOverscrollEffect()
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .fillMaxHeight()
                        .width(
                            width = sheetWidth
                        )
                        .background(
                            color = MaterialTheme.colors.background,
                            shape = screenShape
                        )
                        .clip(screenShape)
                        .clipToBounds()
                ) {
                    body()
                }
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
                if (calculatedPosition != null && calculatedPosition.order < BottomSheetPosition.HIDDEN.order) {
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
    val bottomScreenCornerRadius: Float,
    private val positionalThreshold: (totalDistance: Float) -> Float,
    private val velocityThreshold: () -> Float,
    private val confirmValueChange: (BottomSheetPosition) -> Boolean
) {

    val maxSheetContentSizePercent = 0.95f
    private var anchors: Map<BottomSheetPosition, Float> = emptyMap()
        private set

    var screenHeightPixels: Float = 0f
        private set

    var collapsedInfoHeightPixel: Float = 0f
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
            return fixedPadding
        }

    fun progress(from: BottomSheetPosition, to: BottomSheetPosition): Float {
        return state.progress(from, to)
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
    fun createAnchors(
        screenHeightPixels: Float,
        collapsedInfoHeightPixel: Float,
        collapsedMinimizedInfoHeightPixel: Float,
        expandedTopBarHeightPixel: Float
    ) {
        val anchors = remember(
            screenHeightPixels,
            collapsedInfoHeightPixel,
            collapsedMinimizedInfoHeightPixel,
            expandedTopBarHeightPixel
        ) {
                this.screenHeightPixels = screenHeightPixels
                this.collapsedInfoHeightPixel = collapsedInfoHeightPixel
                anchors = BottomSheetPosition.swipeAnchors(
                    screenHeightPixels = screenHeightPixels,
                    collapsedInfoHeightPixel = collapsedInfoHeightPixel,
                    collapsedMinimizedInfoHeightPixel = collapsedMinimizedInfoHeightPixel,
                    expandedTopBarHeightPixel = expandedTopBarHeightPixel,
                    collapsedAnchorBottomPaddingPixels = collapsedAnchorBottomPaddingPixels,
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

    companion object {
        val sheetHorizontalCollapsedPadding = when (DeviceType.current) {
            DeviceType.Android -> 10.dp
            DeviceType.iOS -> 16.dp
        }
        val sheetHorizontalHalfExpandedPadding = when (DeviceType.current) {
            DeviceType.Android -> 6.dp
            DeviceType.iOS -> 6.dp
        }
        val sheetBottomCollapsedPadding = when (DeviceType.current) {
            DeviceType.Android -> 10.dp
            DeviceType.iOS -> 14.dp
        }
        val sheetBottomCollapsedMinimizedPadding = when (DeviceType.current) {
            DeviceType.Android -> 0.dp
            DeviceType.iOS -> 20.dp
        }
        val sheetHorizontalCollapsedMinimizedPadding = when (DeviceType.current) {
            DeviceType.Android -> 0.dp
            DeviceType.iOS -> 12.dp
        }
        val sheetBottomHalfExpandedPadding = when (DeviceType.current) {
            DeviceType.Android -> 6.dp
            DeviceType.iOS -> 6.dp
        }
        
        // Top corner radius values
        val topCornerCollapsed = when (DeviceType.current) {
            DeviceType.Android -> 50.dp
            DeviceType.iOS -> 50.dp
        }
        val topCornerHalfExpanded = when (DeviceType.current) {
            DeviceType.Android -> 40.dp
            DeviceType.iOS -> 40.dp
        }
        val topCornerExpanded = when (DeviceType.current) {
            DeviceType.Android -> 25.dp
            DeviceType.iOS -> 25.dp
        }
        
        // Bottom corner radius values
        val bottomCornerCollapsed = when (DeviceType.current) {
            DeviceType.Android -> 50.dp
            DeviceType.iOS -> 50.dp
        }
//        val bottomCornerHalfExpanded = when (DeviceType.current) {
//            DeviceType.Android -> 34.dp
//            DeviceType.iOS -> 57.dp
//        }
        val bottomCornerExpanded = when (DeviceType.current) {
            DeviceType.Android -> 0.dp
            DeviceType.iOS -> 0.dp
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberSwipeableSheetState(
    initialValue: BottomSheetPosition = BottomSheetPosition.HIDDEN,
    mapContentListState: LazyListState = rememberLazyListState(),
    collapsedAnchorBottomPaddingPixels: Float = 0f,
    bottomScreenCornerRadius: Float = 0f,
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
            collapsedAnchorBottomPaddingPixels = collapsedAnchorBottomPaddingPixels,
            bottomScreenCornerRadius = bottomScreenCornerRadius
        )
    }
}

sealed interface BottomSheetPosition {
    abstract val order: Int


    data object EXPANDED: BottomSheetPosition {
        override val order: Int = 0
    }

    data object HALF_EXPANDED: BottomSheetPosition {
        override val order: Int = 10
        const val screenHeightPercent: Float = 0.4f
        const val remainingScreenHeightPercent: Float = 1f - screenHeightPercent
    }

    data object COLLAPSED: BottomSheetPosition {
        override val order: Int = 20
    }

    data object COLLAPSED_MINIMISED: BottomSheetPosition {
        override val order: Int = 25
    }

    data object HIDDEN: BottomSheetPosition {
        override val order: Int = 30
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
            COLLAPSED_MINIMISED -> COLLAPSED
            HIDDEN -> COLLAPSED
        }
    }


    companion object {
        fun swipeAnchors(
            screenHeightPixels: Float,
            collapsedInfoHeightPixel: Float,
            collapsedMinimizedInfoHeightPixel: Float,
            collapsedAnchorBottomPaddingPixels: Float,
            expandedTopBarHeightPixel: Float
        ): Map<BottomSheetPosition, Float> {
            val entries = listOf(
                EXPANDED, HALF_EXPANDED, COLLAPSED, COLLAPSED_MINIMISED, HIDDEN
            )
            return entries.associateWith { position ->
                when (position) {
                    EXPANDED -> expandedTopBarHeightPixel

                    HALF_EXPANDED -> {
                        screenHeightPixels.times(HALF_EXPANDED.remainingScreenHeightPercent)
                    }

                    COLLAPSED -> {
                        screenHeightPixels
                            .minus(collapsedInfoHeightPixel)
                            .minus(collapsedAnchorBottomPaddingPixels)
                    }

                    COLLAPSED_MINIMISED -> {
                        screenHeightPixels
                            .minus(collapsedMinimizedInfoHeightPixel)
                            .minus(collapsedAnchorBottomPaddingPixels)
                    }

                    HIDDEN -> screenHeightPixels
                }
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

@OptIn(ExperimentalFoundationApi::class)
fun SwipeableSheetState.isCollapsedAndMinimised(): Boolean {
    return state.currentValue == BottomSheetPosition.COLLAPSED_MINIMISED
}