package app.paralino.core.ui.groups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.presentation.groups.detail.GroupSpaceFocusedFeatureChildItem
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional


@Composable
fun GroupSpaceFocusedFeatureElement(
    modifier: Modifier,
    title: String,
    icon: Painter,
    isActive: Boolean,
    items: List<GroupSpaceFocusedFeatureChildItem>?,
    bottomExtraContent: (@Composable () -> Unit)? = null,
    onChildItemClick: (GroupSpaceFocusedFeatureChildItem) -> Unit
) {
    val backgroundColor = if (isActive) {
        AppColors.accentDark.copy(alpha = 0.1f)
    } else {
        AppColors.redMain.copy(alpha = 0.1f)
    }

    val iconBackgroundColor = if (isActive) {
        if (isSystemInDarkTheme()) {
            AppColors.accentDark.copy(alpha = 0.2f)
        } else {
            AppColors.accentDark.copy(alpha = 0.15f)
        }
    } else {
        AppColors.redMain.copy(alpha = 0.2f)
    }

    val arrowTintColor = if (isActive) {
        AppColors.accentDark.copy(alpha = 0.5f)
    } else {
        AppColors.redMain.copy(alpha = 0.5f)
    }

    val iconTintColor = if (isActive) {
        if (isSystemInDarkTheme()) {
            AppColors.accent
        } else {
            AppColors.accentDark
        }
    } else {
        AppColors.redMain
    }

    val contentColor = if (isActive) {
        if (isSystemInDarkTheme()) {
            MaterialTheme.colors.onBackground
        } else {
            MaterialTheme.colors.primary
        }
    } else {
        AppColors.redMain
    }

    Column(
        modifier = modifier
            .conditional(isActive) {
                shadow(
                    elevation = if (isActive) 12.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = backgroundColor
                )
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) MaterialTheme.colors.background else MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.background)
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colors.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBackgroundColor)
                    .padding(8.dp),
                tint = iconTintColor
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                text = title.annotatedBoldCompose(textColor = contentColor),
                maxLines = 2,
                style = MaterialTheme.typography.subtitle1.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            )

            if (items != null) {
                PlanLimitCount(
                    count = items.size,
                    limit = null
                )
            }

            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                contentDescription = null,
                tint = arrowTintColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!items.isNullOrEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    this.items(items, key = { it.itemId }) { item ->
                        DataInfoChip(
                            modifier = modifier,
                            innerPaddingValues = PaddingValues(
                                start = 4.dp,
                                end = 6.dp,
                                top = 4.dp,
                                bottom = 4.dp
                            ),
                            backgroundColor = AppColors.accent.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(100),
                            imageOrIcon = {
                                ImageModelImage(
                                    imageModel = item.image,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                                            shape = CircleShape
                                        ),
                                    clipShape = CircleShape
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                            },
                            text = {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface
                                )
                            },
                            endSection = {
                                Icon(
                                    modifier = Modifier
                                        .padding(start = 3.dp)
                                        .width(18.dp),
                                    painter = CoreAppIcons.ChevronRight.asPainterResource(),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                                    contentDescription = null
                                )
                            },
                            onClick = { onChildItemClick(item) }
                        )
                    }
                }
            }

            bottomExtraContent?.invoke()
        }
    }
}