package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

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

@Composable
fun PlanLimitSmallUpgradeButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColors.errorBackground,
    contentColor: Color = AppColors.redMain,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.annotatedBoldCompose(
                textColor = contentColor
            ),
            color = contentColor,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )

        Icon(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(24.dp)
                .background(
                    color = AppColors.lightBackground,
                    shape = CircleShape
                )
                .padding(2.dp),
            painter = CoreAppIcons.ChevronRight.asPainterResource(),
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
        )
    }
}