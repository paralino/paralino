package app.paralino.core.ui.theme

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.paralino.core.MR
import dev.icerock.moko.resources.compose.asFont

private val Montserrat @Composable get() = FontFamily(
	MR.fonts.font_montserrat.light.asFont(weight = FontWeight.Light)!!,
	MR.fonts.font_montserrat.regular.asFont(weight = FontWeight.Normal)!!,
	MR.fonts.font_montserrat.semibold.asFont(weight = FontWeight.SemiBold)!!,
	MR.fonts.font_montserrat.bold.asFont(weight = FontWeight.Bold)!!,
)

val AppTypography @Composable get() = Typography(defaultFontFamily = Montserrat)