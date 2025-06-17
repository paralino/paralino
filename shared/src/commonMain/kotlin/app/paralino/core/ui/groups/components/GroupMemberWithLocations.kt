package app.paralino.core.ui.groups.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.LocationRequestItemState
import app.paralino.core.presentation.groups.MemberActionItemState
import app.paralino.core.presentation.groups.MemberActionType
import app.paralino.core.presentation.groups.MemberLocationVisibilityType
import app.paralino.core.provider.NetworkSignalStrength
import app.paralino.core.provider.NetworkType
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.GroupStatusTag
import app.paralino.core.ui.groups.toStatusTagType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional
import app.paralino.core.utils.extensions.pulsate
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupMemberWithLocations(
    state: GroupMemberItemStateWithLocations,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    canShowOldLocation: Boolean = false,
    onEvent: ((GroupMemberItemEvent) -> Unit)? = null
) {

    var expanded by rememberSaveable(state.keyId, forceExpandAction) {
        mutableStateOf(forceExpandAction)
    }

    Column {
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
                ) {
                    ImageModelImage(
                        imageModel = state.memberState.image ?: ImageModel.emoji(Emojis.robot),
                        modifier = Modifier
                            .size(50.dp)
                            .border(
                                width = if (state.memberState.isCurrentUser) 2.dp else 0.5.dp,
                                color = if (state.memberState.isCurrentUser)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        if (state.singleVisibleLocation?.visibleBatteryLevel != null && (!expanded || !state.allowShowingDevices)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 16.dp, height = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .requiredSize(16.dp)
                                            .rotate(90f),
                                        painter = if (state.singleVisibleLocation.isBatteryCharging) {
                                            CoreAppIcons.BatteryChargingFull.asPainterResource()
                                        } else {
                                            CoreAppIcons.BatteryFull.asPainterResource()
                                        },
                                        tint = when {
                                            state.singleVisibleLocation.visibleBatteryLevel > 85 -> AppColors.accent
                                            state.singleVisibleLocation.visibleBatteryLevel < 15 -> AppColors.redMain
                                            state.singleVisibleLocation.visibleBatteryLevel < 30 -> AppColors.highlightGold
                                            else -> MaterialTheme.colors.onSurface
                                        },
                                        contentDescription = null
                                    )
                                }
                                Spacer(modifier = Modifier.size(2.dp))
                                Text(
                                    text = "${state.singleVisibleLocation.visibleBatteryLevel}%",
                                    style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium),
                                    color = when {
                                        state.singleVisibleLocation.visibleBatteryLevel < 15 -> AppColors.redMain
                                        state.singleVisibleLocation.visibleBatteryLevel < 30 -> AppColors.highlightGold
                                        else -> MaterialTheme.colors.onSurface
                                    }
                                )
                            }
                        } else if (state.locations.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    painter = CoreAppIcons.Devices.asPainterResource(),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "${state.locations.size}",
                                    style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .conditional(
                            value = state.singleVisibleLocation?.onlineStatus == MemberDeviceOnlineStatusType.ONLINE
                        ) {
                            pulsate()
                        }
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clip(CircleShape)
                        .border(
                            border = BorderStroke(
                                width = 2.dp,
                                color = when (state.singleVisibleLocation?.onlineStatus) {
                                    MemberDeviceOnlineStatusType.UNKNOWN,
                                    null -> Color.Transparent

                                    else -> MaterialTheme.colors.surface
                                },
                            ),
                            shape = CircleShape
                        )
                        .padding(1.dp)
                        .background(
                            color = when (state.singleVisibleLocation?.onlineStatus) {
                                MemberDeviceOnlineStatusType.ONLINE -> AppColors.accent
                                MemberDeviceOnlineStatusType.BUSY -> AppColors.highlightGold
                                MemberDeviceOnlineStatusType.OFFLINE -> Color.Gray
                                MemberDeviceOnlineStatusType.UNKNOWN,
                                null -> Color.Transparent
                            },
                            shape = CircleShape
                        )
                )
            }

            // center content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val iconId = "deviceIcon"
                Text(
                    text = remember(
                        state.memberState.name,
                        state.singleLocation?.name,
                        expanded,
                        state.allowShowingDevices
                    ) {
                        buildAnnotatedString {
                            this.append(state.memberState.name)
                            if (!state.singleLocation?.name.isNullOrBlank() && (!expanded || !state.allowShowingDevices)) {
                                this.append(" • ")
                                appendInlineContent(iconId, "[device]")
                                this.withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        baselineShift = BaselineShift(0.1f),
                                        color = AppColors.accent
                                    )
                                ) {
                                    this.append(state.singleLocation?.name ?: "")
                                }
                            }
                        }
                    },
                    inlineContent = mapOf(
                        Pair(
                            iconId,
                            InlineTextContent(
                                Placeholder(
                                    width = 14.sp,
                                    height = 16.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) {
                                Icon(
                                    painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                                    contentDescription = null,
                                    tint = AppColors.accent,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 1.dp, end = 1.dp)
                                )
                            }
                        )
                    ),
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                )

                GroupStatusTag(state.toStatusTagType(expanded = expanded))

                if (state.filteredLastPing != null && (!expanded || !state.allowShowingDevices)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = CoreAppIcons.AccessTime.asPainterResource(),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = state.filteredLastPing.annotatedBoldCompose(),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.singleVisibleLocation?.speed != null) {
                        Icon(
                            painter = CoreAppIcons.Speed.asPainterResource(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = state.singleVisibleLocation.speed,
                            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }

                    if (state.singleVisibleLocation?.elevation != null) {
                        Icon(
                            painter = CoreAppIcons.Elevation.asPainterResource(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = state.singleVisibleLocation.elevation,
                            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }

                    if (state.singleVisibleLocation?.networkType != null) {
                        NetworkTypeIcon(
                            state.singleVisibleLocation.networkType,
                            state.singleVisibleLocation.signalStrength
                        )
                        Text(
                            text = state.singleVisibleLocation.networkType.networkName,
                            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }

            val canExpand = state.memberState.canHaveExpandButton &&
                    (state.actions.isActive || (state.locations.isNotEmpty() && state.allowShowingDevices))

            // expand icon
            Box(
                modifier = Modifier
            ) {
                if (canExpand) {
                    Icon(
                        painter = if (expanded) CoreAppIcons.ExpandLess.asPainterResource() else CoreAppIcons.ExpandMore.asPainterResource(),
                        contentDescription = null
                    )
                }
            }
        }

        // action item
        AnimatedVisibility(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .padding(bottom = 10.dp)
                .fillMaxWidth(),
            visible = expanded
        ) {
            Column {

                if (!state.memberState.paralinoId.isNullOrBlank() && state.allowShowingDevices) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onEvent?.invoke(GroupMemberItemEvent.onCopyCode(state.memberState.paralinoId)) }
                                .padding(6.dp),
                            painter = CoreAppIcons.ContentCopy.asPainterResource(),
                            contentDescription = null
                        )
                        Column {
                            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                                Text(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = stringResource(MR.strings.settings_section_personal_code_title),
                                    style = TextStyle(
                                        fontWeight = FontWeight.Light,
                                        fontSize = 12.sp,
                                    )
                                )
                            }
                            Text(
                                modifier = Modifier.padding(start = 4.dp),
                                text = state.memberState.paralinoId,
                                style = TextStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    letterSpacing = 2.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Start,
                                    lineBreak = LineBreak.Heading
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                state.locations.takeIf { state.allowShowingDevices }?.takeIf { it.isNotEmpty() }
                    ?.let {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = stringResource(MR.strings.group_member_devices_section_title),
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
                            )
                            it.forEach {
                                GroupMemberLocationChild(
                                    state = it,
                                    onClick = {
                                        // open device
                                    },
                                    onDelete = {
                                        // delete device
                                        onEvent?.invoke(GroupMemberItemEvent.onDeleteDeviceClick(it))
                                    }
                                )
                            }
                        }
                    }

                if (state.actions.isActive || canShowOldLocation) {
                    GroupMemberActions(
                        modifier = Modifier
                            .padding(top = 0.dp)
                            .fillMaxWidth(),
                        state = state,
                        canShowOldLocation = canShowOldLocation,
                        onEvent = { event ->
                            if (collapseOnActionClick) {
                                expanded = !expanded
                            }
                            logg.v("location_request", "Invoking action: ${event::class}")
                            onEvent?.invoke(event)
                        }
                    )
                }
            }
        }

        if (state.lastStatusPing != null && state.lastStatusPing != state.filteredLastPing && canShowOldLocation) {
            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(MR.strings.group_members_last_status_update).annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                )
                Text(
                    text = " ${state.lastStatusPing}".annotatedBoldCompose(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GroupMemberActions(
    modifier: Modifier = Modifier,
    state: GroupMemberItemStateWithLocations,
    canShowOldLocation: Boolean,
    onEvent: (GroupMemberItemEvent) -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GroupMemberSmallAction(
            modifier = Modifier.align(Alignment.CenterVertically),
            state = state.actions.deleteMember
        ) {
            onEvent(GroupMemberItemEvent.onActionClick(it, state))
        }
        GroupMemberSmallAction(
            modifier = Modifier.align(Alignment.CenterVertically),
            state = state.actions.blockUser
        ) {
            onEvent(GroupMemberItemEvent.onActionClick(it, state))
        }
        GroupMemberSmallAction(
            modifier = Modifier.align(Alignment.CenterVertically),
            state = state.actions.unblockUser
        ) {
            onEvent(GroupMemberItemEvent.onActionClick(it, state))
        }

        if (state.actions.history != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = CoreAppIcons.TimelinePath.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.groups_sharing_setting_timeline_title),
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onActionClick(
                            action = state.actions.history,
                            item = state
                        )
                    )
                }
            )
        }

        if (state.actions.deleteLocation != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = MemberActionType.DELETE_LOCATION.actionIcon,
                endIcon = null,
                isLoading = state.actions.deleteLocation?.isLoading ?: false,
                text = stringResource(MemberActionType.DELETE_LOCATION.actionText),
                backgroundColor = AppColors.errorBackground,
                contentColor = AppColors.redMain,
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onActionClick(
                            action = state.actions.deleteLocation,
                            item = state
                        )
                    )
                }
            )
        }

        if (state.actions.navigate != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = CoreAppIcons.Directions.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.group_members_item_directions_button),
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onDirectionsRequest(
                            action = state.actions.navigate,
                            item = state
                        )
                    )
                }
            )
        }

        if (state.singleLocation?.visibility == MemberLocationVisibilityType.HIDDEN && canShowOldLocation) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = CoreAppIcons.LocationOn.asPainterResource(),
                endIcon = null,
                text = stringResource(MR.strings.group_members_item_old_location_button),
                onClick = {
                    onEvent(GroupMemberItemEvent.onShowLocation(state))
                }
            )
        }

        if (state.actions.locationRequest != null) {
            LocationRequestItem(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth(),
                state = state.actions.locationRequest
            ) {
                onEvent(GroupMemberItemEvent.onLocationRequest(state))
            }
        }

        if (state.actions.securityAction) {
            IconButton(
                onClick = { onEvent(GroupMemberItemEvent.onOpenRawData(state)) }
            ) {
                Icon(
                    painter = CoreAppIcons.Security.asPainterResource(),
                    tint = AppColors.accent,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun GroupMemberSmallAction(
    modifier: Modifier = Modifier,
    state: MemberActionItemState?,
    onClick: (MemberActionItemState) -> Unit
) {
    if (state != null) {
        FilledButton(
            modifier = modifier,
            style = ButtonStyle.SMALL_ACTION,
            icon = state.type.actionIcon,
            elevation = 0.dp,
            onClick = { onClick(state) },
            state = AppButtonState.loadingStateWithCheck(isLoading = state.isLoading)
        ) {
            Text(
                text = stringResource(state.type.actionText),
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupMemberLocationChild(
    modifier: Modifier = Modifier,
    state: GroupMemberItemLocationState,
    onClick: (GroupMemberItemLocationState) -> Unit,
    onDelete: (GroupMemberItemLocationState) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(12.dp))
//            .background(MaterialTheme.colors.surface)
            .combinedClickable(
                onClick = { onClick(state) },
                onLongClick = { if (state.canEdit) onDelete(state) }
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(36.dp)
                    .border(
                        width = if (state.currentDevice) 2.dp else 0.5.dp,
                        color = if (state.currentDevice)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(8.dp),
                painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                contentDescription = null
            )
            if (state.visibleBatteryLevel != null) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 10.dp)
                        ) {
                            Icon(
                                modifier = Modifier
                                    .requiredSize(18.dp)
                                    .rotate(90f),
                                painter = if (state.isBatteryCharging) {
                                    CoreAppIcons.BatteryChargingFull.asPainterResource()
                                } else {
                                    CoreAppIcons.BatteryFull.asPainterResource()
                                },
                                contentDescription = null
                            )
                        }
                        Text(
                            modifier = Modifier.offset(y = -(3.dp)),
                            text = "${state.visibleBatteryLevel}%",
                            style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 8.dp,
                    end = 4.dp,
                    bottom = if (state.visibleBatteryLevel != null && state.lastLocationPing == null) 12.dp else 0.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(MR.strings.group_members_device_name_default),
                style = MaterialTheme.typography.caption,
                modifier = Modifier
            )

            GroupStatusTag(state.deviceStatus?.toStatusTagType())

            if (state.lastLocationPing != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = CoreAppIcons.AccessTime.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(14.dp)
                    )
                    Text(
                        text = state.lastLocationPing.annotatedBoldCompose(),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                if (state.speed != null) {
                    Icon(
                        painter = CoreAppIcons.Speed.asPainterResource(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = state.speed,
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                if (state.elevation != null) {
                    Icon(
                        painter = CoreAppIcons.Elevation.asPainterResource(),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = state.elevation,
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
fun GroupMemberActionButton(
    modifier: Modifier = Modifier,
    startIcon: Painter,
    text: String,
    showText: Boolean = true,
    isLoading: Boolean = false,
    endIcon: Painter? = CoreAppIcons.ChevronRight.asPainterResource(),
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    contentPadding: PaddingValues = PaddingValues(
        start = 8.dp,
        top = 6.dp,
        end = if (endIcon == null) 10.dp else 6.dp,
        bottom = 6.dp
    ),
    onClick: () -> Unit
) {
    DataInfoChip(
        modifier = modifier,
        shape = RoundedCornerShape(100),
        backgroundColor = backgroundColor,
        innerPaddingValues = contentPadding,
        imageOrIcon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = startIcon,
                    contentDescription = null,
                    tint = contentColor
                )
            }
        },
        hasEndArrow = false,
        text = {
            AnimatedVisibility(visible = showText) {
                Text(
                    text = text,
                    color = contentColor,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (endIcon != null) {
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(16.dp),
                    painter = endIcon,
                    tint = contentColor.copy(alpha = ContentAlpha.disabled),
                    contentDescription = null
                )
            }
        },
        onClick = onClick
    )
}

@Composable
private fun LocationRequestItem(
    modifier: Modifier = Modifier,
    state: LocationRequestItemState,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = state.canInteract) {
                logg.v("location_request", "Location request clicked")
                onClick()
            }
            .alpha(if (state.isEnabled) 1f else 0.4f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = if (state.hasError) {
                        CoreAppIcons.Warning.asPainterResource()
                    } else if (!state.isEnabled) {
                        CoreAppIcons.AccessTime.asPainterResource()
                    } else {
                        CoreAppIcons.ShareLocation.asPainterResource()
                    },
                    modifier = Modifier.size(20.dp),
                    contentDescription = null
                )
            }
        }

        Text(
            modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
            text = when {
                state.isWaitingForUser -> stringResource(MR.strings.group_member_device_waiting)
                state.isLoading -> stringResource(MR.strings.group_member_device_requesting)
                state.hasDeviceSetupWrongError -> stringResource(MR.strings.group_member_device_setup_invalid)
                state.hasDeviceUnreachableError -> stringResource(MR.strings.group_member_device_unreachable)
                state.hasRequestError -> stringResource(MR.strings.group_member_device_request_failed)
                state.isStatusRecent -> stringResource(MR.strings.group_members_item_refresh_location_button)
                else -> stringResource(MR.strings.group_members_item_request_location_button)
            },
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
        )

        if (state.isLoading) {
            // nothing
        } else if (!state.isPremiumFeatureEnabled) {
            Icon(
                painter = CoreAppIcons.StarFamily.asPainterResource(),
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = AppColors.highlightGold,
                        shape = CircleShape
                    )
                    .padding(4.dp)
            )
        } else {
            Icon(
                modifier = Modifier.width(20.dp),
                painter = CoreAppIcons.ChevronRight.asPainterResource(),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                contentDescription = null
            )
        }
    }
}

@Composable
fun NetworkTypeIcon(networkType: NetworkType, signalStrength: NetworkSignalStrength?) {
    val tint = remember(signalStrength) {
        when (signalStrength) {
            NetworkSignalStrength.POOR -> Color.Unspecified
            NetworkSignalStrength.GOOD -> Color.Unspecified
            NetworkSignalStrength.GREAT -> AppColors.accent
            null -> null
        }
    }
    val icon = remember(networkType, signalStrength) {
        when (networkType) {
            NetworkType.WIFI -> when (signalStrength) {
                NetworkSignalStrength.POOR -> CoreAppIcons.SignalWifi1
                NetworkSignalStrength.GOOD -> CoreAppIcons.SignalWifi2
                NetworkSignalStrength.GREAT -> CoreAppIcons.SignalWifi3
                null -> CoreAppIcons.SignalWifi3
            }

            NetworkType.GENERIC_CELL,
            NetworkType.G2,
            NetworkType.G3,
            NetworkType.G4,
            NetworkType.G5 -> when (signalStrength) {
                NetworkSignalStrength.POOR -> CoreAppIcons.SignalCell1
                NetworkSignalStrength.GOOD -> CoreAppIcons.SignalCell2
                NetworkSignalStrength.GREAT -> CoreAppIcons.SignalCell3
                null -> CoreAppIcons.SignalCell3
            }
        }
    }
    Icon(
        painter = icon.asPainterResource(),
        contentDescription = null,
        tint = tint ?: MaterialTheme.colors.onSurface,
        modifier = Modifier
            .padding(end = 2.dp)
            .size(18.dp)
    )
}

val MemberActionType.actionIcon: Painter
    @Composable get() = when (this) {
        MemberActionType.DELETE_MEMBER -> CoreAppIcons.PersonRemove.asPainterResource()
        MemberActionType.DELETE_LOCATION -> CoreAppIcons.Delete.asPainterResource()
        MemberActionType.BLOCK_USER -> CoreAppIcons.Block.asPainterResource()
        MemberActionType.UNBLOCK_USER -> CoreAppIcons.LockOpen.asPainterResource()
        MemberActionType.LOCATION_HISTORY -> CoreAppIcons.TimelinePath.asPainterResource()
    }

val MemberActionType.actionText: StringResource
    get() = when (this) {
        MemberActionType.DELETE_MEMBER -> MR.strings.group_members_item_remove_button
        MemberActionType.DELETE_LOCATION -> MR.strings.group_members_item_clear_location_button
        MemberActionType.BLOCK_USER -> MR.strings.group_details_block_group_alert_button
        MemberActionType.UNBLOCK_USER -> MR.strings.blocklist_unblock_alert_button
        MemberActionType.LOCATION_HISTORY -> MR.strings.groups_sharing_setting_timeline_title
    }