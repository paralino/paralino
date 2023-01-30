package app.paralino.android.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.theme.AppColors
import app.paralino.core.presentation.dashboard.GroupItemData

@Composable
fun GroupListItem(
    data: GroupItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageModelImage(
            imageModel = data.image,
            modifier = Modifier
                .size(72.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(12.dp)
                ),
            cornerSize = 12.dp
        )

        Spacer(modifier = Modifier.size(16.dp))
        Column(
            modifier = Modifier
        ) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        modifier = Modifier.size(16.dp),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "${data.userCount}",
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                    )
                }
            }

            val tagColor = when (data.uiType) {
                GroupItemData.GroupUiType.UNVERIFIED -> MaterialTheme.colors.onError
                GroupItemData.GroupUiType.ACTIVE -> AppColors.accent
                else -> MaterialTheme.colors.secondary
            }
            val tagTextColor = MaterialTheme.colors.onPrimary
            val tagText = when (data.uiType) {
                GroupItemData.GroupUiType.UNVERIFIED -> stringResource(id = R.string.dashboard_groups_section_unverified)
                GroupItemData.GroupUiType.ACTIVE -> stringResource(id = R.string.dashboard_groups_section_active)
                GroupItemData.GroupUiType.INACTIVE -> ""
                GroupItemData.GroupUiType.NONE -> ""
            }
            val tagIcon = when (data.uiType) {
                GroupItemData.GroupUiType.UNVERIFIED -> Icons.Rounded.Warning
                GroupItemData.GroupUiType.ACTIVE -> Icons.Rounded.LocationOn
                GroupItemData.GroupUiType.INACTIVE -> null
                GroupItemData.GroupUiType.NONE -> null
            }

            if (tagText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tagIcon != null) {
                        Icon(
                            imageVector = tagIcon,
                            modifier = Modifier.size(16.dp),
                            tint = tagColor,
                            contentDescription = null
                        )
                        Spacer(Modifier.size(4.dp))
                    }
                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = tagTextColor.copy(alpha = 0.8f),
                        modifier = Modifier
                            .background(
                                tagColor,
                                MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}