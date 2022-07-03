package app.paralino.mobile.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.paralino.mobile.R

private val Montserrat = FontFamily(
	Font(R.font.font_montserrat_light, FontWeight.Light),
	Font(R.font.font_montserrat_regular, FontWeight.Normal),
	Font(R.font.font_montserrat_semibold, FontWeight.SemiBold),
	Font(R.font.font_montserrat_bold, FontWeight.Bold)
)

val AppTypography = Typography(defaultFontFamily = Montserrat)