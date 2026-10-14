package app.paralino.core.ui.onboarding.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

@Composable
fun OnboardingChoiceCard(
    modifier: Modifier = Modifier.Companion,
    icon: Painter,
    title: String,
    info: String,
    nextHint: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = if (isSystemInDarkTheme()) 8.dp else 12.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.2f),
                )
            )
            .clip(shape)
            .background(MaterialTheme.colors.surface)
            .border(
                width = 2.dp,
                color = MaterialTheme.colors.background,
                shape = shape
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .background(
                        if (isSystemInDarkTheme()) {
                            AppColors.accentDark.copy(alpha = 0.2f)
                        } else {
                            AppColors.accentDark.copy(alpha = 0.15f)
                        }
                    )
                    .padding(12.dp),
                tint = if (isSystemInDarkTheme()) {
                    AppColors.accent
                } else {
                    AppColors.accentDark
                }
            )
            Icon(
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onBackground,
            )
            Text(
                text = info,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium),
            )
        }

        Text(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(
                    if (isSystemInDarkTheme()) {
                        AppColors.accentDark.copy(alpha = 0.15f)
                    } else {
                        AppColors.accentDark.copy(alpha = 0.1f)
                    }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            text = nextHint.annotatedBoldCompose(),
            style = MaterialTheme.typography.caption,
            color = if (isSystemInDarkTheme()) {
                MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            } else {
                AppColors.accentDark
            },
        )
    }
}