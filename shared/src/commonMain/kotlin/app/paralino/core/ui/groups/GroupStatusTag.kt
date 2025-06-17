package app.paralino.core.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource


enum class GroupStatusTagType {
    SHARING_ACTIVE,
    SHARING_ACTIVE_NO_LOCATION,
    SHARING_ACTIVE_NOT_ACCURATE,
    UNVERIFIED,
    INACTIVE,
    INACTIVE_NOT_ACCURATE,
    NOT_SHARING,
    OFFLINE,
    GPS_OFF,
    BATTERY_SAVING,
    IDLE,
    IDLE_NO_LOCATION,
    IDLE_NOT_ACCURATE,
    TOO_MANY_REQUESTS,
    OVER_LIMITS,
    MISSING_PERMISSION;
}

fun MemberDeviceStatusType.toStatusTagType(): GroupStatusTagType {
    return when (this) {
        MemberDeviceStatusType.GPS_OFF -> GroupStatusTagType.GPS_OFF
        MemberDeviceStatusType.MISSING_PERMISSIONS -> GroupStatusTagType.MISSING_PERMISSION
        MemberDeviceStatusType.NOT_SHARING -> GroupStatusTagType.NOT_SHARING
        MemberDeviceStatusType.BATTERY_SAVER_ON -> GroupStatusTagType.BATTERY_SAVING
        MemberDeviceStatusType.TOO_MANY_REQUESTS -> GroupStatusTagType.TOO_MANY_REQUESTS
        MemberDeviceStatusType.THROTTLING -> GroupStatusTagType.IDLE
        MemberDeviceStatusType.IDLE -> GroupStatusTagType.IDLE
        MemberDeviceStatusType.IDLE_NO_LOCATION -> GroupStatusTagType.IDLE_NO_LOCATION
        MemberDeviceStatusType.ACTIVE -> GroupStatusTagType.SHARING_ACTIVE
        MemberDeviceStatusType.ACTIVE_NO_LOCATION -> GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION
        MemberDeviceStatusType.INACTIVE -> GroupStatusTagType.INACTIVE
    }
}

fun GroupMemberItemStateWithLocations.toStatusTagType(expanded: Boolean): GroupStatusTagType? {
    return when {
        !this.memberState.isVerified -> GroupStatusTagType.UNVERIFIED
        !expanded || this.locations.isEmpty() || !this.allowShowingDevices -> {
            val tagType = this.statusTag?.toStatusTagType()
            if (this.singleVisibleLocation?.accuracyMeters != null && this.singleVisibleLocation.accuracyMeters > 300) {
                when (tagType) {
                    GroupStatusTagType.SHARING_ACTIVE,
                    GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION,
                    GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE -> {
                        GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE
                    }

                    GroupStatusTagType.IDLE,
                    GroupStatusTagType.IDLE_NOT_ACCURATE -> {
                        GroupStatusTagType.IDLE_NOT_ACCURATE
                    }

                    else -> tagType
                }
            } else {
                tagType
            }
        }

        else -> null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupStatusTag(
    status: GroupStatusTagType?
) {
    if (status != null) {

        val tagColor = when (status) {
            GroupStatusTagType.SHARING_ACTIVE,
            GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION,
            GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE -> AppColors.accent

            GroupStatusTagType.UNVERIFIED,
            GroupStatusTagType.GPS_OFF,
            GroupStatusTagType.OVER_LIMITS,
            GroupStatusTagType.MISSING_PERMISSION -> MaterialTheme.colors.error

            GroupStatusTagType.TOO_MANY_REQUESTS,
            GroupStatusTagType.IDLE,
            GroupStatusTagType.IDLE_NO_LOCATION,
            GroupStatusTagType.IDLE_NOT_ACCURATE,
            GroupStatusTagType.INACTIVE_NOT_ACCURATE,
            GroupStatusTagType.BATTERY_SAVING -> AppColors.highlightGold

            GroupStatusTagType.INACTIVE,
            GroupStatusTagType.NOT_SHARING,
            GroupStatusTagType.OFFLINE,
            null -> MaterialTheme.colors.secondary
        }
        val tagTextColor = MaterialTheme.colors.contentColorFor(tagColor)
            .takeOrElse { MaterialTheme.colors.onPrimary }
        val tagText = when (status) {
            GroupStatusTagType.SHARING_ACTIVE -> stringResource(resource = MR.strings.group_members_status_active)
            GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION -> ""

            GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE -> ""
            GroupStatusTagType.IDLE_NOT_ACCURATE -> ""
            GroupStatusTagType.INACTIVE_NOT_ACCURATE -> ""

            GroupStatusTagType.OVER_LIMITS -> stringResource(resource = MR.strings.plan_limit_contact_owner_alert_title)
            GroupStatusTagType.UNVERIFIED -> stringResource(resource = MR.strings.dashboard_groups_section_unverified)
            GroupStatusTagType.INACTIVE -> stringResource(resource = MR.strings.group_members_status_inactive)
            GroupStatusTagType.NOT_SHARING -> stringResource(resource = MR.strings.group_members_status_not_sharing)

            GroupStatusTagType.GPS_OFF -> stringResource(resource = MR.strings.group_members_status_gps_off)
            GroupStatusTagType.MISSING_PERMISSION -> stringResource(resource = MR.strings.group_members_status_missing_permissions)
            GroupStatusTagType.BATTERY_SAVING -> stringResource(resource = MR.strings.group_members_status_battery_saver)
            GroupStatusTagType.OFFLINE -> stringResource(resource = MR.strings.group_members_status_offline)
            GroupStatusTagType.TOO_MANY_REQUESTS -> stringResource(resource = MR.strings.group_members_status_too_many_requests)
            GroupStatusTagType.IDLE,
            GroupStatusTagType.IDLE_NO_LOCATION -> stringResource(resource = MR.strings.group_members_status_throttle)

            null -> ""
        }

        val inaccurateLocationText = when (status) {
            GroupStatusTagType.INACTIVE_NOT_ACCURATE,
            GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE,
            GroupStatusTagType.IDLE_NOT_ACCURATE -> stringResource(resource = MR.strings.group_members_status_location_inaccurate)

            else -> ""
        }

        val tagIcon = when (status) {
            GroupStatusTagType.SHARING_ACTIVE,
            GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION,
            GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE,
            GroupStatusTagType.IDLE_NOT_ACCURATE,
            GroupStatusTagType.INACTIVE_NOT_ACCURATE,
            GroupStatusTagType.IDLE -> CoreAppIcons.LocationOn.asPainterResource()

            GroupStatusTagType.GPS_OFF,
            GroupStatusTagType.MISSING_PERMISSION,
            GroupStatusTagType.TOO_MANY_REQUESTS -> CoreAppIcons.LocationOff.asPainterResource()

            GroupStatusTagType.OVER_LIMITS,
            GroupStatusTagType.UNVERIFIED -> CoreAppIcons.Warning.asPainterResource()

            GroupStatusTagType.BATTERY_SAVING -> CoreAppIcons.BatteryChargingFull.asPainterResource()

            GroupStatusTagType.INACTIVE,
            GroupStatusTagType.IDLE_NO_LOCATION,
            GroupStatusTagType.OFFLINE,
            GroupStatusTagType.NOT_SHARING -> null

            null -> null
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            if (tagIcon != null) {
                Icon(
                    painter = tagIcon,
                    modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
                    tint = tagColor,
                    contentDescription = null
                )
            }

            if (status == GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION ||
                status == GroupStatusTagType.IDLE_NOT_ACCURATE ||
                status == GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
                    strokeWidth = 2.dp,
                    color = tagColor
                )
            }

            if (tagText.isNotBlank()) {
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
                        .align(Alignment.CenterVertically)
                )
            }

            if (inaccurateLocationText.isNotBlank()) {
                Text(
                    text = inaccurateLocationText,
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.darkBackground.copy(alpha = 0.8f),
                    modifier = Modifier
                        .background(
                            AppColors.highlightGold,
                            MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}