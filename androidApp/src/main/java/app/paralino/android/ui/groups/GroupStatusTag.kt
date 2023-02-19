package app.paralino.android.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.ui.theme.AppIcons


enum class GroupStatusTagType {
    SHARING_ACTIVE, UNVERIFIED, INACTIVE
}

@Composable
fun GroupStatusTag(
    status: GroupStatusTagType?
) {

    val tagColor = when(status) {
        GroupStatusTagType.SHARING_ACTIVE -> AppColors.accent
        GroupStatusTagType.UNVERIFIED -> MaterialTheme.colors.onError
        GroupStatusTagType.INACTIVE -> MaterialTheme.colors.secondary
        else -> MaterialTheme.colors.secondary
    }
    val tagTextColor = MaterialTheme.colors.onPrimary
    val tagText = when(status) {
        GroupStatusTagType.SHARING_ACTIVE -> stringResource(id = R.string.dashboard_groups_section_active)
        GroupStatusTagType.UNVERIFIED -> stringResource(id = R.string.dashboard_groups_section_unverified)
        GroupStatusTagType.INACTIVE -> stringResource(id = R.string.group_members_status_inactive)
        else -> ""
    }
    val tagIcon = when(status) {
        GroupStatusTagType.SHARING_ACTIVE -> AppIcons.LocationOn
        GroupStatusTagType.UNVERIFIED -> AppIcons.Warning
        GroupStatusTagType.INACTIVE -> null
        else -> null
    }

    if (status != null) {
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