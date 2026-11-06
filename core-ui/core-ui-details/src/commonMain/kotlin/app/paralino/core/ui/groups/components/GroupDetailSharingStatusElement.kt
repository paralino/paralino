package app.paralino.core.ui.groups.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun GroupDetailSharingStatusElement(
    modifier: Modifier = Modifier,
    contract: LocationSharingState,
    isPersonalSpace: Boolean
) {
    val backgroundColor = MaterialTheme.colors.background
    val contentColor = contentColorFor(backgroundColor = backgroundColor)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(percent = 50)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sharingTime = contract.endTime
                ?.let { stringResource(MR.strings.group_details_sharing_status_time, it) }
                ?: let {
                    if (isPersonalSpace) {
                        stringResource(MR.strings.personalSpace_tracking_status_infinite)
                    } else {
                        stringResource(MR.strings.group_details_sharing_status_infinite)
                    }
                }

            Icon(
                painter = CoreAppIcons.Timer.asPainterResource(),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = sharingTime.annotatedBoldCompose(),
                style = MaterialTheme.typography.caption.copy(color = contentColor)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (contract.shareBattery) {
                Icon(
                    painter = CoreAppIcons.BatteryChargingFull.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareSpeed) {
                Icon(
                    painter = CoreAppIcons.Speed.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareElevation) {
                Icon(
                    painter = CoreAppIcons.Elevation.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareNetwork) {
                Icon(
                    painter = CoreAppIcons.SignalCell3.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareDeviceStatus) {
                Icon(
                    painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }


            if (contract.saveToHistory) {
                Icon(
                    painter = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.approximateLocation != null) {
                Icon(
                    painter = CoreAppIcons.NotListedLocation.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 5.dp, end = 2.dp)
                        .padding(vertical = 5.dp)
                        .size(18.dp)
                )
                Text(
                    text = "(${contract.approximateLocation})",
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
                Spacer(modifier = Modifier.size(5.dp))
            }
        }

    }
}