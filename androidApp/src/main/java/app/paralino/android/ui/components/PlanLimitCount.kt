package app.paralino.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.ui.theme.AppColors

@Composable
fun PlanLimitCount(
    count: Int,
    limit: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            this.withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    baselineShift = BaselineShift(0.1f),
                    color = if(isSystemInDarkTheme()) AppColors.accent else AppColors.accentDark
                )
            ) {
                this.append("$count")
            }
            if(limit >= count) {
                this.withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        baselineShift = BaselineShift(0.1f),
                        color = AppColors.accentDark
                    )
                ) {
                    this.append("/${limit}")
                }
            }
        },
        style = MaterialTheme.typography.subtitle1,
        modifier = modifier
            .background(
                color = if (limit == count) MaterialTheme.colors.error
                else AppColors.accentLight.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(vertical = 2.dp, horizontal = 6.dp)
    )
}