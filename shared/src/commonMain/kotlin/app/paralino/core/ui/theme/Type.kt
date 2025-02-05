package app.paralino.core.ui.theme

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.paralino.core.MR
import dev.icerock.moko.resources.compose.asFont

private val Montserrat @Composable get() = FontFamily(
	MR.fonts.font_montserrat_light.asFont(weight = FontWeight.Light)!!,
	MR.fonts.font_montserrat_regular.asFont(weight = FontWeight.Normal)!!,
	MR.fonts.font_montserrat_medium.asFont(weight = FontWeight.Medium)!!,
	MR.fonts.font_montserrat_semibold.asFont(weight = FontWeight.SemiBold)!!,
	MR.fonts.font_montserrat_bold.asFont(weight = FontWeight.Bold)!!,
	MR.fonts.font_montserrat_extrabold.asFont(weight = FontWeight.ExtraBold)!!,
)

val AppTypography @Composable get() = Typography(defaultFontFamily = Montserrat)