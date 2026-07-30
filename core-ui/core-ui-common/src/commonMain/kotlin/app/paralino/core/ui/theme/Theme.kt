package app.paralino.core.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paralino.core.provider.ComposeLocalUriHandler
import app.paralino.core.provider.ComposeUriHandler

private val LightThemeColors = lightColors(
	background = AppColors.lightBackground,
	surface = AppColors.lightSurface,
	primary = AppColors.lightThemePrimary,
	secondary = AppColors.lightThemeSecondary,
	error = AppColors.redMain,
	primaryVariant = AppColors.darkSurface,
	secondaryVariant = AppColors.lightThemeSecondary,
	onPrimary = AppColors.lightBackground,
	onSecondary = AppColors.darkBackground,
	onError = AppColors.lightBackground,
	onBackground = AppColors.darkBackground,
	onSurface = AppColors.darkBackground
)

private val DarkThemeColors = darkColors(
	background = AppColors.darkBackground,
	surface = AppColors.darkSurface,
	primary = AppColors.darkThemePrimary,
	secondary = AppColors.darkThemeSecondary,
	error = AppColors.redMain,
	primaryVariant = AppColors.lightSurface,
	secondaryVariant = AppColors.darkThemeSecondary,
	onPrimary = AppColors.lightBackground,
	onSecondary = AppColors.lightBackground,
	onError = AppColors.lightBackground,
	onBackground = AppColors.lightBackground,
	onSurface = AppColors.lightBackground
)

private val shapes = Shapes(
	small = RoundedCornerShape(percent = 25),
	medium = RoundedCornerShape(20.dp),
	large = RoundedCornerShape(percent = 0)
)

sealed interface ButtonStyle {

	val defaultModifier: Modifier
	val paddingValues: PaddingValues
	val shape: Shape
	val iconSize: Dp
	val iconSpacing: Dp
	val minContentHeight: Dp

	companion object {
		val BIG = BIG()
		val SMALL = SMALL()
		val ICON = ICON()
		val SMALL_ACTION = SMALL_ACTION()
	}

	data class BIG(
		override val defaultModifier: Modifier = Modifier.fillMaxWidth(),
		override val paddingValues: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
		override val shape: CornerBasedShape = shapes.small,
		override val iconSize: Dp = 18.dp,
		override val iconSpacing: Dp = 8.dp,
		override val minContentHeight: Dp = 24.dp,
	): ButtonStyle

	data class SMALL(
		override val defaultModifier: Modifier = Modifier,
		override val paddingValues: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
		override val shape: CornerBasedShape = RoundedCornerShape(percent = 50),
		override val iconSize: Dp = 18.dp,
		override val iconSpacing: Dp = 8.dp,
		override val minContentHeight: Dp = 24.dp,
	): ButtonStyle

	data class ICON(
		override val defaultModifier: Modifier = Modifier,
		override val paddingValues: PaddingValues = PaddingValues(),
		override val shape: CornerBasedShape = RoundedCornerShape(percent = 50),
		override val iconSize: Dp = 18.dp,
		override val iconSpacing: Dp = 0.dp,
		override val minContentHeight: Dp = 20.dp,
	): ButtonStyle

	data class SMALL_ACTION(
		override val defaultModifier: Modifier = Modifier,
		override val paddingValues: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
		override val shape: CornerBasedShape = RoundedCornerShape(percent = 50),
		override val iconSize: Dp = 12.dp,
		override val iconSpacing: Dp = 4.dp,
		override val minContentHeight: Dp = 14.dp,
	): ButtonStyle
}

@OptIn(ExperimentalMaterialApi::class)
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

	CompositionLocalProvider(LocalRippleConfiguration provides AppRippleTheme.RippleConfig()) {
		MaterialTheme(
			colors = colors,
			typography = AppTypography.Montserrat,
			shapes = shapes
		){
			BoxWithConstraints {
				CompositionLocalProvider(
					LocalIndication provides ScaleIndication,
					ComposeLocalUriHandler provides remember { ComposeUriHandler() },
					LocalScreenDimensionType provides screenDimensionFromScope(),
					content = content
				)
			}
		}
	}
}

private object AppRippleTheme  {
	@OptIn(ExperimentalMaterialApi::class)
	@Composable
	fun RippleConfig(): RippleConfiguration {
		val contentLuminance = LocalContentColor.current.luminance()
		val lightTheme = MaterialTheme.colors.isLight
		val contentColor = LocalContentColor.current

		val rippleColor = remember (contentLuminance, lightTheme) {
			when {
				!lightTheme && contentLuminance < 0.3 -> {
					Color.Black
				}
				else -> {
					Color.Unspecified
				}
			}
		}

		return remember (contentLuminance, lightTheme, contentColor) {
			RippleConfiguration(
				color = rippleColor,
				rippleAlpha = RippleDefaults.rippleAlpha(contentColor = contentColor, lightTheme = false)
			)
		}
	}
}