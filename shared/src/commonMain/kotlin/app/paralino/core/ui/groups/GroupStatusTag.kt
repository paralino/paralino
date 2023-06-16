package app.paralino.core.ui.groups

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource


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
        GroupStatusTagType.SHARING_ACTIVE -> stringResource(resource = MR.strings.dashboard_groups_section_active)
        GroupStatusTagType.UNVERIFIED -> stringResource(resource = MR.strings.dashboard_groups_section_unverified)
        GroupStatusTagType.INACTIVE -> stringResource(resource = MR.strings.group_members_status_inactive)
        else -> ""
    }
    val tagIcon = when(status) {
        GroupStatusTagType.SHARING_ACTIVE -> CoreAppIcons.LocationOn.asPainterResource()
        GroupStatusTagType.UNVERIFIED -> CoreAppIcons.Warning.asPainterResource()
        GroupStatusTagType.INACTIVE -> null
        else -> null
    }

    if (status != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tagIcon != null) {
                Icon(
                    painter = tagIcon,
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