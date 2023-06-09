package app.paralino.android.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.core.utils.ConditionErrorType
import app.paralino.android.R
import app.paralino.android.ui.theme.AppIcons
import app.paralino.core.ui.theme.AppTheme

@Composable
fun ConditionErrorItem(
    modifier: Modifier = Modifier,
    data: ConditionErrorType,
    onClick: () -> Unit,
) {
    ConditionErrorItem(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.error,
        iconVector = data.iconVector,
        text = stringResource(id = data.nameRes),
        endAction = if(data.endActionTextRes != 0) stringResource(id = data.endActionTextRes) else "",
        onClick = onClick
    )
}

@Composable
fun ConditionErrorItem(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.error,
    contentColor: Color = contentColorFor(backgroundColor = backgroundColor),
    iconVector: ImageVector?,
    text: String,
    endAction: String?,
    onClick: () -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {

        val (icon, infoText, action) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(icon) {
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                },
        ) {
            if(iconVector != null) {
                Icon(
                    imageVector = iconVector,
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
            modifier = Modifier.constrainAs(infoText) {
                centerVerticallyTo(parent)
                linkTo(
                    start = icon.end,
                    end = action.start,
                    startMargin = 8.dp,
                    endMargin = 8.dp,
                    endGoneMargin = 0.dp,
                    bias = 0f
                )
                width = Dimension.preferredWrapContent
            }
        )

        if (endAction != null) {
            Text(
                text = endAction,
                color = MaterialTheme.colors.onPrimary,
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .constrainAs(action) {
                        centerVerticallyTo(parent)
                        end.linkTo(parent.end)
                    }
                    .background(
                        color = MaterialTheme.colors.primary,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            )
        }
    }
}

@Preview(widthDp = 320, heightDp = 200)
@Preview(widthDp = 320, heightDp = 200, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ErrorItemPreview() {
    AppTheme {
        Surface {
            Column {
                ConditionErrorItem(data = ConditionErrorType.PERMISSION_LOCATION) {}
                ConditionErrorItem(data = ConditionErrorType.NETWORK) {}
            }
        }
    }
}

val ConditionErrorType.iconVector: ImageVector
    @Composable
    get() = when (this) {
        ConditionErrorType.GPS -> AppIcons.LocationOff
        ConditionErrorType.PERMISSION_NOTIFICATIONS -> AppIcons.NotificationsActive
        ConditionErrorType.PERMISSION_LOCATION -> AppIcons.PhonelinkSetup
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> AppIcons.PhonelinkSetup
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> AppIcons.PhonelinkSetup
        ConditionErrorType.NETWORK -> AppIcons.WifiOff
        is ConditionErrorType.EMAIL_VERIFICATION -> AppIcons.Email
        ConditionErrorType.GUEST_ACCOUNT -> ImageVector.vectorResource(id = R.drawable.ic_incognito)
    }

val ConditionErrorType.nameRes: Int
    get() = when (this) {
        ConditionErrorType.GPS -> R.string.location_banner_error_gps_off
        ConditionErrorType.PERMISSION_NOTIFICATIONS -> R.string.location_banner_error_no_permission_notifications
        ConditionErrorType.PERMISSION_LOCATION -> R.string.location_banner_error_no_permission_location
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> R.string.location_banner_error_no_permission_location
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> R.string.location_banner_error_no_permission_location_background
        ConditionErrorType.NETWORK -> R.string.location_banner_error_offline
        is ConditionErrorType.EMAIL_VERIFICATION -> R.string.info_email_confirm
        ConditionErrorType.GUEST_ACCOUNT -> R.string.settings_account_guest_badge
    }

val ConditionErrorType.endActionTextRes: Int
    get() = when (this) {
        ConditionErrorType.GPS -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_NOTIFICATIONS -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> R.string.location_banner_fix
        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> R.string.location_banner_fix
        ConditionErrorType.NETWORK -> 0
        is ConditionErrorType.EMAIL_VERIFICATION -> R.string.button_verify
        ConditionErrorType.GUEST_ACCOUNT -> R.string.location_banner_fix
    }