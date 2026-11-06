package app.paralino.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.map.MapMarkerStyle
import app.paralino.core.presentation.map.MapPinData
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.isLocationInaccurate
import dev.icerock.moko.resources.compose.stringResource

fun MapPinData.hasInfoWindowContent(): Boolean {
    val hasTimeInfo = !timeInfo.isNullOrBlank()
    val hasAccuracyInfo =
        pinType == MapPinType.MEMBER &&
            markerStyle == MapMarkerStyle.CIRCLE &&
            !title.isNullOrBlank()
    return hasTimeInfo || hasAccuracyInfo
}

@Composable
fun MapMarkerInfoWindowContent(
    modifier: Modifier = Modifier,
    state: MapPinData,
) {
    val timeShow = !state.timeInfo.isNullOrBlank()
    val accuracyInfoShow =
        state.pinType == MapPinType.MEMBER &&
            state.markerStyle == MapMarkerStyle.CIRCLE &&
            !state.title.isNullOrBlank()
    if (!state.hasInfoWindowContent()) {
        // maps-compose 8.5.0+ crashes if info window content measures 0x0
        Box(modifier = modifier.size(1.dp))
        return
    }

    Column(
        modifier = modifier
            .padding(horizontal = 40.dp)
            .background(
                color = MaterialTheme.colors.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(percent = 12)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (accuracyInfoShow) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = CoreAppIcons.CircleWithOutline.asPainterResource(),
                    tint = AppColors.accentDark.copy(alpha = 0.8f),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(18.dp)
                )
                Text(
                    text = if (state.accuracy.isLocationInaccurate()) {
                        MR.strings.group_members_marker_circle_info_inaccurate.localizedString(
                            state.title
                                ?: MR.strings.group_member_device_title.localizedString()
                        )
                    } else {
                        MR.strings.group_members_marker_circle_info.localizedString(
                            state.title
                                ?: MR.strings.group_member_device_title.localizedString()
                        )
                    }
                )
            }

            if (state.accuracy.isLocationInaccurate()) {
                SmallActionButton(
                    modifier = Modifier.padding(top = 4.dp),
                    backgroundColor = AppColors.highlightGold,
                    contentColor = AppColors.darkBackground.copy(alpha = 0.8f),
                    icon = CoreAppIcons.NotListedLocation.asPainterResource(),
                    text = {
                        Text(
                            text = if (state.isApproximate) {
                                stringResource(resource = MR.strings.group_members_status_location_hidden)
                            } else {
                                stringResource(resource = MR.strings.group_members_status_location_inaccurate)
                            },
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    },
                    onClick = null
                )
            }
        }

        if (accuracyInfoShow && timeShow) {
            Divider(
                modifier = Modifier.padding(8.dp)
            )
        }

        if (timeShow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = CoreAppIcons.AccessTime.asPainterResource(),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 2.dp, end = 4.dp)
                        .size(14.dp)
                )
                Text(
                    text = state.timeInfo ?: "",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                )
            }
        }
    }
}
