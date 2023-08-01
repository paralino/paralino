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
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource


enum class GroupStatusTagType {
    SHARING_ACTIVE,
    UNVERIFIED,
    INACTIVE,
    NOT_SHARING,
    OFFLINE,
    GPS_OFF,
    BATTERY_SAVING,
    IDLE,
    TOO_MANY_REQUESTS,
    MISSING_PERMISSION;
}

fun MemberDeviceStatusType.toStatusTagType(): GroupStatusTagType {
    return when (this) {
        MemberDeviceStatusType.GPS_OFF -> GroupStatusTagType.GPS_OFF
        MemberDeviceStatusType.MISSING_PERMISSIONS -> GroupStatusTagType.MISSING_PERMISSION
        MemberDeviceStatusType.NOT_SHARING -> GroupStatusTagType.NOT_SHARING
        MemberDeviceStatusType.BATTERY_SAVER_ON -> GroupStatusTagType.BATTERY_SAVING
        MemberDeviceStatusType.TOO_MANY_REQUESTS -> GroupStatusTagType.TOO_MANY_REQUESTS
        MemberDeviceStatusType.IDLE -> GroupStatusTagType.IDLE
        MemberDeviceStatusType.ACTIVE -> GroupStatusTagType.SHARING_ACTIVE
        MemberDeviceStatusType.INACTIVE -> GroupStatusTagType.INACTIVE
    }
}

@Composable
fun GroupStatusTag(
    status: GroupStatusTagType?
) {
    if (status != null) {

        val tagColor = when (status) {
            GroupStatusTagType.SHARING_ACTIVE -> AppColors.accent
            GroupStatusTagType.UNVERIFIED,
            GroupStatusTagType.GPS_OFF,
            GroupStatusTagType.MISSING_PERMISSION -> MaterialTheme.colors.onError

            GroupStatusTagType.TOO_MANY_REQUESTS,
            GroupStatusTagType.IDLE,
            GroupStatusTagType.BATTERY_SAVING -> AppColors.highlightGold

            GroupStatusTagType.INACTIVE,
            GroupStatusTagType.NOT_SHARING,
            GroupStatusTagType.OFFLINE,
            null -> MaterialTheme.colors.secondary
        }
        val tagTextColor = MaterialTheme.colors.onPrimary
        val tagText = when (status) {
            GroupStatusTagType.SHARING_ACTIVE -> stringResource(resource = MR.strings.group_members_status_active)
            GroupStatusTagType.UNVERIFIED -> stringResource(resource = MR.strings.dashboard_groups_section_unverified)
            GroupStatusTagType.INACTIVE,
            GroupStatusTagType.NOT_SHARING -> stringResource(resource = MR.strings.group_members_status_inactive)

            GroupStatusTagType.GPS_OFF -> stringResource(resource = MR.strings.group_members_status_gps_off)
            GroupStatusTagType.MISSING_PERMISSION -> stringResource(resource = MR.strings.group_members_status_missing_permissions)
            GroupStatusTagType.BATTERY_SAVING -> stringResource(resource = MR.strings.group_members_status_battery_saver)
            GroupStatusTagType.OFFLINE -> stringResource(resource = MR.strings.group_members_status_offline)
            GroupStatusTagType.TOO_MANY_REQUESTS -> stringResource(resource = MR.strings.group_members_status_throttle)
            GroupStatusTagType.IDLE -> stringResource(resource = MR.strings.group_members_status_throttle)
            null -> ""
        }
        val tagIcon = when (status) {
            GroupStatusTagType.SHARING_ACTIVE,
            GroupStatusTagType.GPS_OFF,
            GroupStatusTagType.MISSING_PERMISSION,
            GroupStatusTagType.TOO_MANY_REQUESTS,
            GroupStatusTagType.IDLE -> CoreAppIcons.LocationOn.asPainterResource()

            GroupStatusTagType.UNVERIFIED -> CoreAppIcons.Warning.asPainterResource()
            GroupStatusTagType.BATTERY_SAVING -> CoreAppIcons.BatteryChargingFull.asPainterResource()
            GroupStatusTagType.INACTIVE, GroupStatusTagType.OFFLINE, GroupStatusTagType.NOT_SHARING -> null
            null -> null
        }

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