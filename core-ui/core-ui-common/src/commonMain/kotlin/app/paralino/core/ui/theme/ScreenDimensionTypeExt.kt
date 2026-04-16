package app.paralino.core.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.common.ScreenDimensionType

val LocalScreenDimensionType = compositionLocalOf { ScreenDimensionType(0.dp, 0.dp) }

@Composable
fun BoxWithConstraintsScope.screenDimensionFromScope(): ScreenDimensionType {
	return remember(this) {
		ScreenDimensionType(scope = this)
	}
}