package app.paralino.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.utils.ConditionErrorType
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ConditionErrorItem(
    modifier: Modifier = Modifier,
    data: ConditionErrorType,
    onClick: () -> Unit,
) {
    ConditionErrorItem(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.error,
        icon = data.icon,
        text = stringResource(data.nameRes),
        endAction = if (data.endActionTextRes != null)
            stringResource(data.endActionTextRes!!)
        else null,
        endActionLoading = false,
        onClick = onClick
    )
}

@Composable
fun ConditionErrorItem(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.error,
    contentColor: Color = contentColorFor(backgroundColor = backgroundColor),
    icon: Painter?,
    text: String,
    endAction: String?,
    endActionLoading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier,
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    modifier = Modifier
                        .size(16.dp),
                    tint = contentColor,
                    contentDescription = null
                )
            }
        }

        Text(
            text = text,
            color = contentColor,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f)
        )

        if (endAction != null) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                    text = endAction,
                    color = animateColorAsState(if (endActionLoading) Color.Transparent else MaterialTheme.colors.onPrimary).value,
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                )
                if (endActionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}

//@Preview(widthDp = 320, heightDp = 200)
//@Preview(widthDp = 320, heightDp = 200, uiMode = UI_MODE_NIGHT_YES)
//@Composable
//fun ErrorItemPreview() {
//    AppTheme {
//        Surface {
//            Column {
//                ConditionErrorItem(data = ConditionErrorType.PERMISSION_LOCATION) {}
//                ConditionErrorItem(data = ConditionErrorType.NETWORK) {}
//            }
//        }
//    }
//}

val ConditionErrorType.icon: Painter
    @Composable
    get() = when (this) {
        ConditionErrorType.GPS -> CoreAppIcons.LocationOff
        ConditionErrorType.PermissionNotifications -> CoreAppIcons.NotificationsActive
        is ConditionErrorType.PermissionLocation -> CoreAppIcons.PhonelinkSetup
        ConditionErrorType.PermissionLocationBackground -> CoreAppIcons.PhonelinkSetup
        ConditionErrorType.Network -> CoreAppIcons.WifiOff
        is ConditionErrorType.EmailVerification -> CoreAppIcons.Email
        ConditionErrorType.GuestAccount -> CoreAppIcons.Incognito
    }.asPainterResource()

val ConditionErrorType.nameRes: StringResource
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_error_gps_off
        ConditionErrorType.PermissionNotifications -> MR.strings.location_banner_error_no_permission_notifications
        is ConditionErrorType.PermissionLocation -> MR.strings.location_banner_error_no_permission_location
        ConditionErrorType.PermissionLocationBackground -> MR.strings.location_banner_error_no_permission_location_background
        ConditionErrorType.Network -> MR.strings.location_banner_error_offline
        is ConditionErrorType.EmailVerification -> MR.strings.info_email_confirm
        ConditionErrorType.GuestAccount -> MR.strings.settings_account_guest_badge
    }

val ConditionErrorType.endActionTextRes: StringResource?
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_fix
        ConditionErrorType.PermissionNotifications -> MR.strings.location_banner_fix
        is ConditionErrorType.PermissionLocation -> MR.strings.location_banner_fix
        ConditionErrorType.PermissionLocationBackground -> MR.strings.location_banner_fix
        ConditionErrorType.Network -> null
        is ConditionErrorType.EmailVerification -> MR.strings.button_verify
        ConditionErrorType.GuestAccount -> MR.strings.location_banner_fix
    }