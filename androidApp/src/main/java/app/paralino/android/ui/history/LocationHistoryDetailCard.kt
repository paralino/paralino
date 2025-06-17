package app.paralino.android.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberNavigateItemState
import app.paralino.core.presentation.history.LocationHistoryMapFocusType
import app.paralino.core.presentation.history.LocationHistoryPointState
import app.paralino.core.presentation.history.LocationHistoryScreenState
import app.paralino.core.presentation.history.LocationHistoryUiEvent
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.EmbeddedSlider
import app.paralino.core.ui.components.EmbeddedSliderSize
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.GroupStatusTag
import app.paralino.core.ui.groups.GroupStatusTagType
import app.paralino.core.ui.groups.components.GroupMemberActionButton
import app.paralino.core.ui.groups.components.NetworkTypeIcon
import app.paralino.core.ui.groups.components.actionIcon
import app.paralino.core.ui.groups.components.actionText
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationHistoryDetailCard(
    modifier: Modifier = Modifier,
    uiState: LocationHistoryScreenState,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
//            .clickable { }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.background)
//            .animateContentSize(
//                animationSpec = spring(
//                    dampingRatio = Spring.DampingRatioLowBouncy,
//                    stiffness = Spring.StiffnessMedium
//                )
//            ),
    ) {
        LazyRow(
            modifier = Modifier
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp)
        ) {
            this.items(uiState.devices, key = { it.locationId }) { device ->
                UserDeviceChip(
                    modifier = Modifier.padding(end = 8.dp),
                    selected = device.isSelected,
                    name = device.deviceName,
                    onClick = {
                        onEvent(LocationHistoryUiEvent.SelectDevice(device = device))
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // image
            Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .heightIn(min = 50.dp)
                ) {
                    if (uiState.userImage != null) {
                        ImageModelImage(
                            imageModel = uiState.userImage!!,
                            modifier = Modifier
                                .size(50.dp)
                                .border(
                                    width = if (uiState.isCurrentUser) {
                                        2.dp
                                    } else {
                                        0.5.dp
                                    },
                                    color = if (uiState.isCurrentUser) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }

            // center content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.userName ?: "",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                )

                when (uiState.mapFocusType) {
                    LocationHistoryMapFocusType.LocationPoint -> {
                        uiState.selectedPoint?.let { point ->
                            LocationTime(time = point.locationTime)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SpeedAndElevationData(point = point)
                            }

                            AnimatedVisibility(visible = point.point.loc != null && point.point.loc!!.acc > 300) {
                                GroupStatusTag(status = GroupStatusTagType.INACTIVE_NOT_ACCURATE)
                            }
                        }
                    }

                    LocationHistoryMapFocusType.Stats -> {
                        LocationTime(time = uiState.stats?.formattedTimeRange)
                    }

                    LocationHistoryMapFocusType.Timeline -> {}
                }
            }
        }

        AnimatedVisibility(visible = uiState.mapFocusType == LocationHistoryMapFocusType.Stats) {
            uiState.stats?.let {
                LocationHistoryStatsTable(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .padding(horizontal = 10.dp),
                    stats = it
                )
            }
        }

        AnimatedVisibility(visible = uiState.mapFocusType == LocationHistoryMapFocusType.LocationPoint) {
            Column {
                uiState.selectedPoint?.let { point ->
                    AnimatedVisibility(point.statusTime != null) {
                        FlowRow(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(MR.strings.group_members_last_status_update).annotatedBoldCompose(),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                            Text(
                                text = point.statusTime ?: "",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .padding(bottom = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatteryAndNetworkData(point = point)
                    }
                }

                GroupMemberActions(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp),
                    id = uiState.locationIndex.toString(),
                    deleteAction = uiState.deleteAction,
                    navigateAction = uiState.navigateAction,
                    onEvent = onEvent
                )

                val sliderSize = remember {
                    EmbeddedSliderSize(minStepWidth = 80.dp, minHeight = 40.dp)
                }
                EmbeddedSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp)
                        .height(sliderSize.minHeight),
                    title = uiState.locationIndex.plus(1).toString(),
                    value = null,
                    size = sliderSize,
                    progress = uiState.locationIndex,
                    shape = RoundedCornerShape(8.dp),
                    steps = uiState.selectedLocations.size,
                    onProgressChange = {
                        onEvent(LocationHistoryUiEvent.SetHistoryProgress(it))
                    }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.LocationTime(
    modifier: Modifier = Modifier,
    time: String?
) {
    if (time != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = CoreAppIcons.AccessTime.asPainterResource(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(16.dp)
            )
            Text(
                text = time.annotatedBoldCompose(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun UserDeviceChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    name: String,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colors.onPrimary
        else MaterialTheme.colors.onSurface
    )
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colors.primary
        else MaterialTheme.colors.surface
    )

    DataInfoChip(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        backgroundColor = backgroundColor,
        imageOrIcon = {
            Icon(
                modifier = Modifier.size(18.dp),
                painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                contentDescription = null,
                tint = contentColor
            )
        },
        hasEndArrow = false,
        text = {
            Text(
                text = name,
                color = contentColor,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun RowScope.SpeedAndElevationData(
    point: LocationHistoryPointState
) {
    if (point.formattedSpeed != null) {
        Icon(
            painter = CoreAppIcons.Speed.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 2.dp)
                .size(18.dp)
        )
        Text(
            text = point.formattedSpeed!!,
            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }

    if (point.formattedElevation != null) {
        Icon(
            painter = CoreAppIcons.Elevation.asPainterResource(),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 2.dp)
                .size(18.dp)
        )
        Text(
            text = point.formattedElevation!!,
            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun RowScope.BatteryAndNetworkData(
    point: LocationHistoryPointState
) {
    if (point.point.status.bat != null) {
        Icon(
            painter = if (point.point.status.chrg == 1) {
                CoreAppIcons.BatteryFull.asPainterResource()
            } else {
                CoreAppIcons.BatteryChargingFull.asPainterResource()
            },
            contentDescription = null,
            modifier = Modifier
                .padding(end = 2.dp)
                .size(18.dp)
        )
        Text(
            text = "${point.point.status.bat}%",
            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }

    if (point.networkType != null) {
        NetworkTypeIcon(
            point.networkType!!,
            point.signalStrength
        )
        Text(
            text = point.networkType!!.networkName,
            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GroupMemberActions(
    modifier: Modifier = Modifier,
    id: String,
    deleteAction: MemberActionItemState?,
    navigateAction: MemberNavigateItemState?,
    onEvent: (LocationHistoryUiEvent) -> Unit
) {
    var isDeleteVisible by remember(id) {
        mutableStateOf(false)
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (deleteAction != null) {
            GroupMemberActionButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .align(Alignment.CenterVertically),
                startIcon = MemberActionType.DELETE_LOCATION.actionIcon,
                endIcon = null,
                isLoading = deleteAction.isLoading,
                contentPadding = PaddingValues(
                    start = animateDpAsState(if (isDeleteVisible) 8.dp else 6.dp).value,
                    top = 6.dp,
                    end = animateDpAsState(if (isDeleteVisible) 10.dp else 6.dp).value,
                    bottom = 6.dp
                ),
                text = stringResource(MemberActionType.DELETE_LOCATION.actionText),
                showText = isDeleteVisible,
                backgroundColor = animateColorAsState(if (isDeleteVisible) AppColors.errorBackground else MaterialTheme.colors.surface).value,
                contentColor = animateColorAsState(if (isDeleteVisible) AppColors.redMain else MaterialTheme.colors.onSurface).value,
                onClick = {
                    if (isDeleteVisible) {
                        onEvent(LocationHistoryUiEvent.onDeleteSelectedLocationRequest)
                    } else {
                        isDeleteVisible = true
                    }
                }
            )
        }

        if (navigateAction != null) {
            GroupMemberActionButton(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .align(Alignment.CenterVertically),
                startIcon = CoreAppIcons.Directions.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.group_members_item_directions_button),
                onClick = {
                    onEvent(
                        LocationHistoryUiEvent.onDirectionsRequest(action = navigateAction)
                    )
                }
            )
        }
    }
}