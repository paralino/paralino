package app.paralino.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.models.base.DeviceType
import app.paralino.core.provider.current
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose

@Composable
fun GenericSettingsOptionItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String?,
    batteryImpact: String?,
    highBatteryImpact: Boolean,
    isSelected: Boolean,
    isLoading: Boolean,
    isPremiumFeatureEnabled: Boolean,
    onClick: () -> Unit
) {
    val sourceColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
    val backgroundColor by animateColorAsState(targetValue = sourceColor)
    val contentColor by animateColorAsState(targetValue = contentColorFor(backgroundColor = sourceColor))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = isLoading,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = !isLoading,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                CheckButtonIcon(
                    modifier = Modifier.fillMaxSize(),
                    isChecked = isSelected,
                    backgroundCheckedColor = MaterialTheme.colors.onPrimary,
                    contentCheckedColor = MaterialTheme.colors.primary,
                    backgroundUncheckedColor = backgroundColor,
                    contentUncheckedColor = contentColor.copy(alpha = ContentAlpha.disabled)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .alpha(if (isPremiumFeatureEnabled) 1f else 0.5f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1.copy(
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (DeviceType.current == DeviceType.iOS) 18.sp else 24.sp,
                )
            )

            if (batteryImpact != null) {
                SmallActionButton(
                    icon = null,
                    backgroundColor = if (highBatteryImpact) {
                        MaterialTheme.colors.error
                    } else {
                        AppColors.accent.copy(alpha = 0.4f)
                    },
                    contentColor = if (highBatteryImpact) {
                        MaterialTheme.colors.onError
                    } else {
                        animateColorAsState(
                            if (isSelected) {
                                AppColors.lightBackground.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                            }
                        ).value
                    },
                    text = {
                        Text(
                            text = batteryImpact,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    compactMode = true,
                    onClick = null
                )
            }

            if (description != null) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = description.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Start,
                        color = contentColor
                    )
                }
            }
        }

        if (!isPremiumFeatureEnabled) {
            Icon(
                painter = CoreAppIcons.StarFamily.asPainterResource(),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .background(
                        color = AppColors.highlightGold,
                        shape = CircleShape
                    )
                    .padding(4.dp)
            )
        }
    }
}
