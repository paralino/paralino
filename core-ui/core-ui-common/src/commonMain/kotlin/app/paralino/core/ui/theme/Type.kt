package app.paralino.core.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import dev.icerock.moko.resources.compose.asFont

object AppTypography {
	private val FontMontserrat @Composable get() = FontFamily(
		MR.fonts.font_montserrat_light.asFont(weight = FontWeight.Light)!!,
		MR.fonts.font_montserrat_regular.asFont(weight = FontWeight.Normal)!!,
		MR.fonts.font_montserrat_medium.asFont(weight = FontWeight.Medium)!!,
		MR.fonts.font_montserrat_semibold.asFont(weight = FontWeight.SemiBold)!!,
		MR.fonts.font_montserrat_bold.asFont(weight = FontWeight.Bold)!!,
		MR.fonts.font_montserrat_extrabold.asFont(weight = FontWeight.ExtraBold)!!,
	)

	private val FontCatamaran @Composable get() = FontFamily(
		MR.fonts.font_catamaran_semibold.asFont(weight = FontWeight.SemiBold)!!,
	)

	val Montserrat @Composable get() = Typography(defaultFontFamily = FontMontserrat)
	val Catamaran @Composable get() = Typography(defaultFontFamily = FontCatamaran)
}

val Typography.featureItemTitle @Composable get() = MaterialTheme.typography.subtitle2.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)