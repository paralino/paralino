package app.paralino.core.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

val LocalScreenDimensionType = compositionLocalOf { ScreenDimensionType(0.dp, 0.dp) }

@Composable
fun BoxWithConstraintsScope.screenDimensionFromScope(): ScreenDimensionType {
	return remember(this) {
		ScreenDimensionType(scope = this)
	}
}