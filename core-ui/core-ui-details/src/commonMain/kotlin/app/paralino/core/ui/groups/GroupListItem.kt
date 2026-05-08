package app.paralino.core.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.presentation.common.GroupItemState
import app.paralino.core.presentation.common.GroupStatusTagType
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.theme.AppColors

@Composable
fun GroupListItem(
    modifier: Modifier = Modifier,
    data: GroupItemState,
    onEdit: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (data.isPersonalSpace) AppColors.accent.copy(0.1f) else MaterialTheme.colors.surface
            )
            .run {
                if (data.isSelected) {
                    this.border(
                        width = 2.dp,
                        color = MaterialTheme.colors.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else this
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(
                    start = if (data.isSelected) 0.dp else 12.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (data.isSelected) {
                Icon(
                    painter = CoreAppIcons.Check.asPainterResource(),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onPrimary,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(width = 16.dp, height = 20.dp)
                        .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
                        .background(MaterialTheme.colors.primary)
                        .padding(1.dp)
                )
            }

            ImageModelImage(
                imageModel = data.image,
                modifier = Modifier
                    .size(if (data.isLarge) 72.dp else 50.dp)
                    .border(
                        width = if (data.isPersonalSpace) 2.dp else 0.5.dp,
                        color = if (data.isPersonalSpace) {
                            AppColors.accentDark.copy(0.3f)
                        } else {
                            MaterialTheme.colors.secondary.copy(
                                alpha = ContentAlpha.disabled
                            )
                        },
                        if (data.isPersonalSpace) CircleShape else RoundedCornerShape(10.dp)
                    ),
                useBackgroundForEmoji = false,
                clipShape = if (data.isPersonalSpace) CircleShape else RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.h5.copy(
                        fontSize = if (data.isLarge) 20.sp else 16.sp,
                        fontWeight = if (data.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    modifier = Modifier
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (data.membersCount != null) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                            Icon(
                                painter = CoreAppIcons.Group.asPainterResource(),
                                modifier = Modifier.size(16.dp),
                                contentDescription = null
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                text = "${data.membersCount}",
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                    }

                    if (data.devicesCount != null) {
                        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                            Icon(
                                painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                                modifier = Modifier.size(16.dp),
                                contentDescription = null
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                text = "${data.devicesCount}",
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                    }

                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                        Icon(
                            painter = CoreAppIcons.MapsHomeWork.asPainterResource(),
                            modifier = Modifier.size(16.dp),
                            contentDescription = null
                        )
                        Spacer(Modifier.size(2.dp))
                        Text(
                            text = "${data.placesCount}",
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                        )
                    }
                }

                val status = when (data.uiType) {
                    GroupItemState.GroupUiType.OVER_LIMITS -> GroupStatusTagType.OVER_LIMITS
                    GroupItemState.GroupUiType.UNVERIFIED -> GroupStatusTagType.UNVERIFIED
                    GroupItemState.GroupUiType.ACTIVE -> GroupStatusTagType.SHARING_ACTIVE
                    GroupItemState.GroupUiType.INACTIVE -> null
                }
                GroupStatusTag(status)
            }

            if (data.canEdit) {
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, end = 12.dp)
                        .size(height = 50.dp, width = 0.5.dp)
                        .background(
                            color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                        )
                )
                Icon(
                    painter = CoreAppIcons.Settings.asPainterResource(),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onEdit() }
                        .clip(CircleShape)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                            shape = CircleShape
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}