package app.paralino.core.presentation.map

import kotlin.math.absoluteValue

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
        val expandedAnchor = anchors[EXPANDED]
        val collapsedAnchor = anchors[COLLAPSED]

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