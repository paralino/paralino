package app.paralino.core.ui.groups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
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
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.common.GroupStatusTagType
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupStatusTag(
    modifier: Modifier = Modifier,
    status: GroupStatusTagType?,
    extraMessage: String? = null
) {
    if (status != null) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            GroupStatusTagContent(status = status, extraMessage = extraMessage)
        }
    }
}

@Composable
fun RowScope.GroupStatusTagContent(
    status: GroupStatusTagType,
    extraMessage: String?
) {
    val tagIconColor = when (status) {
        GroupStatusTagType.SHARING_ACTIVE,
        GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION,
        GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE,
        GroupStatusTagType.SHARING_ACTIVE_LOCATION_HIDDEN,
        GroupStatusTagType.SHARING_ACTIVE_PRIVACY_ZONE -> AppColors.accent

        GroupStatusTagType.UNVERIFIED,
        GroupStatusTagType.GPS_OFF,
        GroupStatusTagType.OVER_LIMITS,
        GroupStatusTagType.MISSING_PERMISSION -> MaterialTheme.colors.error

        GroupStatusTagType.TOO_MANY_REQUESTS,
        GroupStatusTagType.IDLE,
        GroupStatusTagType.IDLE_NO_LOCATION,
        GroupStatusTagType.IDLE_NOT_ACCURATE,
        GroupStatusTagType.INACTIVE_NOT_ACCURATE,
        GroupStatusTagType.IDLE_LOCATION_HIDDEN,
        GroupStatusTagType.IDLE_PRIVACY_ZONE,
        GroupStatusTagType.INACTIVE_LOCATION_HIDDEN,
        GroupStatusTagType.INACTIVE_PRIVACY_ZONE,
        GroupStatusTagType.BATTERY_SAVING -> AppColors.highlightGold

        GroupStatusTagType.INACTIVE,
        GroupStatusTagType.NOT_SHARING,
        GroupStatusTagType.OFFLINE -> MaterialTheme.colors.secondary
    }

    val tagBackgroundColor = when (status) {
        GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE,
        GroupStatusTagType.SHARING_ACTIVE_LOCATION_HIDDEN,
        GroupStatusTagType.SHARING_ACTIVE_PRIVACY_ZONE -> AppColors.highlightGold
        else -> tagIconColor
    }

    val tagTextColor = when (tagBackgroundColor) {
        AppColors.highlightGold -> AppColors.darkBackground

        else -> MaterialTheme.colors.contentColorFor(tagBackgroundColor)
            .takeOrElse { MaterialTheme.colors.onPrimary }
    }

    val tagText = when (status) {
        GroupStatusTagType.SHARING_ACTIVE -> stringResource(resource = MR.strings.group_members_status_active)
        GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION -> ""

        GroupStatusTagType.INACTIVE_NOT_ACCURATE,
        GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE,
        GroupStatusTagType.IDLE_NOT_ACCURATE -> stringResource(resource = MR.strings.group_members_status_location_inaccurate)

        GroupStatusTagType.SHARING_ACTIVE_LOCATION_HIDDEN,
        GroupStatusTagType.IDLE_LOCATION_HIDDEN,
        GroupStatusTagType.INACTIVE_LOCATION_HIDDEN -> stringResource(resource = MR.strings.group_members_status_location_hidden)

        GroupStatusTagType.SHARING_ACTIVE_PRIVACY_ZONE,
        GroupStatusTagType.IDLE_PRIVACY_ZONE,
        GroupStatusTagType.INACTIVE_PRIVACY_ZONE -> {
            extraMessage ?: stringResource(resource = MR.strings.group_members_status_privacy_zone)
        }

        GroupStatusTagType.OVER_LIMITS -> stringResource(resource = MR.strings.plan_limit_alert_title)
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
    }

    val tagIcon = when (status) {
        GroupStatusTagType.SHARING_ACTIVE,
        GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE,
        GroupStatusTagType.SHARING_ACTIVE_LOCATION_HIDDEN,
        GroupStatusTagType.SHARING_ACTIVE_PRIVACY_ZONE,
        GroupStatusTagType.IDLE_NOT_ACCURATE,
        GroupStatusTagType.IDLE_LOCATION_HIDDEN,
        GroupStatusTagType.IDLE_PRIVACY_ZONE,
        GroupStatusTagType.INACTIVE_NOT_ACCURATE,
        GroupStatusTagType.INACTIVE_LOCATION_HIDDEN,
        GroupStatusTagType.INACTIVE_PRIVACY_ZONE,
        GroupStatusTagType.IDLE -> null

        GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION -> CoreAppIcons.ShareLocation.asPainterResource()

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
    }

    val hasLoadingProgress = status == GroupStatusTagType.SHARING_ACTIVE_NO_LOCATION ||
            status == GroupStatusTagType.IDLE_NOT_ACCURATE ||
            status == GroupStatusTagType.SHARING_ACTIVE_NOT_ACCURATE

    if (tagIcon != null || hasLoadingProgress) {
        Box(
            modifier = Modifier.align(Alignment.CenterVertically),
            contentAlignment = Alignment.Center
        ) {
            if (tagIcon != null) {
                Icon(
                    painter = tagIcon,
                    modifier = Modifier.size(18.dp),
                    tint = tagIconColor,
                    contentDescription = null
                )
            }

            if (hasLoadingProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = tagIconColor
                )
            }
        }
    }

    if (tagText.isNotBlank()) {
        Text(
            text = tagText,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = tagTextColor.copy(alpha = 0.8f),
            modifier = Modifier
                .background(
                    tagBackgroundColor,
                    MaterialTheme.shapes.medium
                )
                .padding(horizontal = 6.dp, vertical = 1.dp)
                .align(Alignment.CenterVertically)
        )
    }
}