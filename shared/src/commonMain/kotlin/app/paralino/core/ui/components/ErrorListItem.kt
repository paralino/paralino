package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.ui.theme.AppTheme
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
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
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
            Text(
                text = endAction,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            )
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
        ConditionErrorType.PERMISSION_NOTIFICATIONS -> CoreAppIcons.NotificationsActive
        ConditionErrorType.PERMISSION_LOCATION -> CoreAppIcons.PhonelinkSetup
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> CoreAppIcons.PhonelinkSetup
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> CoreAppIcons.PhonelinkSetup
        ConditionErrorType.NETWORK -> CoreAppIcons.WifiOff
        is ConditionErrorType.EMAIL_VERIFICATION -> CoreAppIcons.Email
        ConditionErrorType.GUEST_ACCOUNT -> CoreAppIcons.Incognito
    }

val ConditionErrorType.nameRes: StringResource
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_error_gps_off
        ConditionErrorType.PERMISSION_NOTIFICATIONS -> MR.strings.location_banner_error_no_permission_notifications
        ConditionErrorType.PERMISSION_LOCATION -> MR.strings.location_banner_error_no_permission_location
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> MR.strings.location_banner_error_no_permission_location
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> MR.strings.location_banner_error_no_permission_location_background
        ConditionErrorType.NETWORK -> MR.strings.location_banner_error_offline
        is ConditionErrorType.EMAIL_VERIFICATION -> MR.strings.info_email_confirm
        ConditionErrorType.GUEST_ACCOUNT -> MR.strings.settings_account_guest_badge
    }

val ConditionErrorType.endActionTextRes: StringResource?
    get() = when (this) {
        ConditionErrorType.GPS -> MR.strings.location_banner_fix
        ConditionErrorType.PERMISSION_NOTIFICATIONS -> MR.strings.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION -> MR.strings.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> MR.strings.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> MR.strings.location_banner_fix
        ConditionErrorType.NETWORK -> null
        is ConditionErrorType.EMAIL_VERIFICATION -> MR.strings.button_verify
        ConditionErrorType.GUEST_ACCOUNT -> MR.strings.location_banner_fix
    }