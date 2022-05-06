package com.zbapp.locationtrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val LightThemeColors = lightColors(
	background = AppColors.lightBackground,
	surface = AppColors.lightSurface,
	primary = AppColors.lightThemePrimary,
	secondary = AppColors.darkThemeSecondary,
	error = AppColors.errorBackground,
	primaryVariant = AppColors.lightThemePrimary,
	secondaryVariant = AppColors.lightThemeSecondary,
	onPrimary = AppColors.lightBackground,
	onSecondary = AppColors.lightSurface,
	onError = AppColors.redMain,
	onBackground = AppColors.lightThemePrimary,
	onSurface = AppColors.lightThemePrimary
)

private val DarkThemeColors = darkColors(
	background = AppColors.darkBackground,
	surface = AppColors.darkSurface,
	primary = AppColors.darkThemePrimary,
	secondary = AppColors.darkThemeSecondary,
	error = AppColors.errorBackground,
	primaryVariant = AppColors.darkThemePrimary,
	secondaryVariant = AppColors.darkThemeSecondary,
	onPrimary = AppColors.darkBackground,
	onSecondary = AppColors.darkSurface,
	onError = AppColors.redMain,
	onBackground = AppColors.darkThemePrimary,
	onSurface = AppColors.darkThemePrimary
)

private val shapes = Shapes(
	small = RoundedCornerShape(percent = 30),
	medium = RoundedCornerShape(20.dp),
	large = RoundedCornerShape(percent = 10)
)

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