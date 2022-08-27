package app.paralino.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LightThemeColors = lightColors(
	background = AppColors.lightBackground,
	surface = AppColors.lightSurface,
	primary = AppColors.lightThemePrimary,
	secondary = AppColors.lightThemeSecondary,
	error = AppColors.errorBackground,
	primaryVariant = AppColors.darkSurface,
	secondaryVariant = AppColors.lightThemeSecondary,
	onPrimary = AppColors.lightBackground,
	onSecondary = AppColors.lightSurface,
	onError = AppColors.redMain,
	onBackground = AppColors.darkBackground,
	onSurface = AppColors.darkBackground
)

private val DarkThemeColors = darkColors(
	background = AppColors.darkBackground,
	surface = AppColors.darkSurface,
	primary = AppColors.darkThemePrimary,
	secondary = AppColors.darkThemeSecondary,
	error = AppColors.errorBackground,
	primaryVariant = AppColors.lightSurface,
	secondaryVariant = AppColors.darkThemeSecondary,
	onPrimary = AppColors.darkBackground,
	onSecondary = AppColors.darkSurface,
	onError = AppColors.redMain,
	onBackground = AppColors.lightBackground,
	onSurface = AppColors.lightBackground
)

private val shapes = Shapes(
	small = RoundedCornerShape(percent = 25),
	medium = RoundedCornerShape(20.dp),
	large = RoundedCornerShape(percent = 0)
)

enum class ButtonStyle(
	val defaultModifier: Modifier,
	val paddingValues: PaddingValues,
	val shape: Shape,
	val iconSize: Dp,
	val iconSpacing: Dp,
	val minContentHeight: Dp
) {
	BIG(
		defaultModifier = Modifier.fillMaxWidth(),
		paddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
		shape = shapes.small,
		iconSize = 18.dp,
		iconSpacing = 8.dp,
		minContentHeight = 24.dp,
	),
	SMALL(
		defaultModifier = Modifier,
		paddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
		shape = RoundedCornerShape(percent = 50),
		iconSize = 18.dp,
		iconSpacing = 8.dp,
		minContentHeight = 24.dp
	),
	SMALL_ACTION(
		defaultModifier = Modifier,
		paddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
		shape = RoundedCornerShape(percent = 50),
		iconSpacing = 4.dp,
		iconSize = 12.dp,
		minContentHeight = 14.dp
	)
}

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightThemeColors
    } else {
        DarkThemeColors
    }

    MaterialTheme(
        colors = colors,
        typography = AppTypography,
		shapes = shapes
    ){
		CompositionLocalProvider(
			LocalRippleTheme provides AppRippleTheme,
			content = content
		)
	}
}

private object AppRippleTheme : RippleTheme {

	@Composable
	override fun defaultColor(): Color {
		val lightTheme = MaterialTheme.colors.isLight
		val contentLuminance = LocalContentColor.current.luminance()

		return when {
			!lightTheme && contentLuminance < 0.3 -> {
				Color.Black
			}
			else -> {
				return RippleTheme.defaultRippleColor(LocalContentColor.current, MaterialTheme.colors.isLight)
			}
		}
	}

	@Composable
	override fun rippleAlpha(): RippleAlpha {
		return RippleTheme.defaultRippleAlpha(LocalContentColor.current, false)
	}
}