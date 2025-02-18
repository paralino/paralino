package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.ui.theme.AppColors

@Composable
fun PlanLimitCount(
    modifier: Modifier = Modifier,
    count: Int,
    limit: Int?,
    backgroundColor: Color = AppColors.accentLight.copy(alpha = 0.3f),
    errorBackgroundColor: Color = AppColors.errorBackground,
    contentColor: Color = if(isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark
) {
    Text(
        text = remember(count, limit, contentColor) {
            buildAnnotatedString {
                this.withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = contentColor
                    )
                ) {
                    this.append("$count")
                }
                if(limit != null) {
                    this.withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = contentColor
                        )
                    ) {
                        this.append("/${limit}")
                    }
                }
            }
        },
        style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
        modifier = modifier
            .background(
                color = if (limit != null && limit <= count) errorBackgroundColor
                else backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(vertical = 2.dp, horizontal = 6.dp)
    )
}