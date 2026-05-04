package app.paralino.core.presentation.common

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.ui.unit.Dp

data class ScreenDimensionType(
    val maxHeightDp: Dp,
    val maxWidthDp: Dp
) {
	val isReady = maxHeightDp.value > 0 && maxHeightDp.value > 0
	val isSmall = maxHeightDp.value < 460

	constructor(scope: BoxWithConstraintsScope): this(
		maxHeightDp = scope.maxHeight,
		maxWidthDp = scope.maxWidth
	)
}