package app.paralino.core.ui.groups.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.logg
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberDeviceOnlineStatusType
import app.paralino.core.presentation.groups.GroupMemberItemEvent
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemState
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
import app.paralino.core.ui.groups.GroupStatusTagContent
import app.paralino.core.ui.groups.toStatusTagType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.Emojis
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.annotatedBoldCompose
import app.paralino.core.utils.extensions.conditional
import app.paralino.core.utils.extensions.pulsate
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupMemberWithLocations(
    state: GroupMemberItemState,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    canShowOldLocation: Boolean = false,
    onEvent: ((GroupMemberItemEvent) -> Unit)? = null
) {

    var expanded by rememberSaveable(state.keyId, forceExpandAction) {
        mutableStateOf(forceExpandAction)
    }
    val singleVisibleLocation = state.singleVisibleLocation

    Column {
        Row(
            modifier = Modifier
                .padding(
                    top = 10.dp,
                    bottom = if (state.compactMultiDeviceMode) 0.dp else 10.dp
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // image
            Box(modifier = Modifier.width(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier
                        .width(if (state.compactMultiDeviceMode) 32.dp else 50.dp)
                ) {
                    ImageModelImage(
                        imageModel = state.memberState.image ?: ImageModel.emoji(Emojis.robot),
                        modifier = Modifier
                            .size(if (state.compactMultiDeviceMode) 32.dp else 50.dp)
                            .border(
                                width = if (state.isHighlighted) 2.dp else 0.5.dp,
                                color = if (state.isHighlighted)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
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
                    .padding(
                        start = if (state.compactMultiDeviceMode) 8.dp else 12.dp,
                        end = 8.dp
                    ),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically
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
                                this.append(state.memberState.name.annotatedBold())
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
                }

                val isLastPingVisible =
                    state.filteredLastPing != null && (!expanded || !state.allowShowingDevices)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        if (state.locations.size > 1 && !state.compactMultiDeviceMode) {
                            Row(
                                // offset to make it visually look in place
                                modifier = Modifier.offset(y = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.size(2.dp))
                                Text(
                                    text = "${state.locations.size}",
                                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }

                    state.toStatusTagType(expanded = expanded)?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start)
                        ) {
                            GroupStatusTagContent(it)
                        }
                    }

                    if (isLastPingVisible) {
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
                                text = (state.filteredLastPing ?: "").annotatedBoldCompose(),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    } else {
                        GroupMemberDeviceDataRowContent(
                            state = singleVisibleLocation,
                            isBatteryVisible = (!expanded || !state.allowShowingDevices)
                        )
                    }
                }

                if (isLastPingVisible) {
                    GroupMemberDeviceDataFlowRow(
                        modifier = Modifier.padding(top = 2.dp),
                        state = singleVisibleLocation,
                        isBatteryVisible = (!expanded || !state.allowShowingDevices)
                    )
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
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(22.dp)
                            .rotate(if (expanded) 90f else 0f),
                        painter = CoreAppIcons.ChevronRight.asPainterResource(),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                        contentDescription = null
                    )
                }
            }
        }

        // action item
        AnimatedVisibility(
            visible = expanded
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth(),
            ) {
                val memberState = state.memberState
                if (!memberState.paralinoId.isNullOrBlank() && state.allowShowingDevices) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    onEvent?.invoke(
                                        GroupMemberItemEvent.onCopyCode(
                                            memberState.paralinoId
                                        )
                                    )
                                }
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
                                text = memberState.paralinoId,
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
                    ?.let { devices ->
                        Column(
                            modifier = Modifier.padding(
                                top = if (!state.compactMultiDeviceMode) 16.dp else 0.dp,
                                bottom = 8.dp
                            )
                        ) {
                            if (!state.compactMultiDeviceMode) {
                                Text(
                                    modifier = Modifier.padding(start = 8.dp),
                                    text = stringResource(MR.strings.group_member_devices_section_title),
                                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium)
                                )
                            }

                            devices.forEach { device ->
                                GroupMemberLocationChild(
                                    state = device,
                                    onClick = {
                                        // open device
                                        onEvent?.invoke(
                                            GroupMemberItemEvent.onDeviceClick(
                                                item = state,
                                                device = it
                                            )
                                        )
                                    },
                                    onDelete = {
                                        // delete device
                                        onEvent?.invoke(
                                            GroupMemberItemEvent.onDeleteDeviceClick(
                                                item = state,
                                                device = it
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                if (state.actions.isActive || canShowOldLocation) {
                    GroupMemberActions(
                        modifier = Modifier
                            .padding(bottom = 10.dp)
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
    state: GroupMemberItemState,
    canShowOldLocation: Boolean,
    onEvent: (GroupMemberItemEvent) -> Unit
) {
    val actions = state.actions
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (actions.deleteMember != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = actions.deleteMember.type.actionIcon,
                endIcon = null,
                isLoading = actions.deleteMember.isLoading,
                text = stringResource(actions.deleteMember.type.actionText),
                backgroundColor = AppColors.errorBackground,
                contentColor = AppColors.redMain,
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onActionClick(
                            action = actions.deleteMember,
                            item = state
                        )
                    )
                }
            )
        }

        if (actions.blockUser != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = actions.blockUser.type.actionIcon,
                endIcon = null,
                isLoading = actions.blockUser.isLoading,
                text = stringResource(actions.blockUser.type.actionText),
                backgroundColor = AppColors.errorBackground,
                contentColor = AppColors.redMain,
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onActionClick(
                            action = actions.blockUser,
                            item = state
                        )
                    )
                }
            )
        }

        if (actions.unblockUser != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = actions.unblockUser.type.actionIcon,
                endIcon = null,
                isLoading = actions.unblockUser.isLoading,
                text = stringResource(actions.unblockUser.type.actionText),
                backgroundColor = if (isSystemInDarkTheme()) {
                    AppColors.accentLight.copy(alpha = 0.2f)
                } else {
                    AppColors.accent.copy(alpha = 0.2f)
                },
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onActionClick(
                            action = actions.unblockUser,
                            item = state
                        )
                    )
                }
            )
        }

        if (actions.deleteLocation != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = MemberActionType.DELETE_LOCATION.actionIcon,
                endIcon = null,
                isLoading = actions.deleteLocation.isLoading,
                text = stringResource(MemberActionType.DELETE_LOCATION.actionText),
                backgroundColor = AppColors.errorBackground,
                contentColor = AppColors.redMain,
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onActionClick(
                            action = actions.deleteLocation,
                            item = state
                        )
                    )
                }
            )
        }

        if (actions.history != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.group_members_item_history_button),
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onOpenLocationHistory(item = state)
                    )
                }
            )
        }

        if (actions.navigate != null) {
            GroupMemberActionButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                startIcon = CoreAppIcons.Directions.asPainterResource(),
                endIcon = CoreAppIcons.ChevronRight.asPainterResource(),
                text = stringResource(MR.strings.group_members_item_directions_button),
                onClick = {
                    onEvent(
                        GroupMemberItemEvent.onDirectionsRequest(
                            action = actions.navigate,
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

        if (actions.locationRequest != null) {
            LocationRequestItem(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth(),
                state = actions.locationRequest
            ) {
                onEvent(GroupMemberItemEvent.onLocationRequest(state))
            }
        }

        if (actions.securityAction) {
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
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
//            .background(MaterialTheme.colors.surface)
            .clickable(
                onClick = { onClick(state) }
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 8.dp,
                    end = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = state.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(MR.strings.group_members_device_name_default),
                style = MaterialTheme.typography.caption,
                modifier = Modifier
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                state.deviceStatus?.toStatusTagType()?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start)
                    ) {
                        GroupStatusTagContent(it)
                    }
                }

                if (state.lastLocationPing != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                } else {
                    GroupMemberDeviceDataRowContent(
                        state = state,
                        isBatteryVisible = true
                    )
                }
            }

            if (state.lastLocationPing != null) {
                GroupMemberDeviceDataFlowRow(
                    modifier = Modifier,
                    state = state,
                    isBatteryVisible = true
                )
            }
        }

        if (state.canEdit) {
            GroupMemberActionButton(
                modifier = Modifier,
                startIcon = CoreAppIcons.Delete.asPainterResource(),
                endIcon = null,
                isLoading = false,
                contentPadding = PaddingValues(6.dp),
                text = "",
                showText = false,
                backgroundColor = AppColors.errorBackground,
                contentColor = AppColors.redMain,
                onClick = { onDelete(state) }
            )
        }
    }
}

@Composable
fun GroupMemberDeviceDataFlowRow(
    modifier: Modifier,
    state: GroupMemberItemLocationState?,
    isBatteryVisible: Boolean
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        GroupMemberDeviceDataRowContent(
            state = state,
            isBatteryVisible = isBatteryVisible
        )
    }
}

@Composable
fun GroupMemberDeviceDataFlowRow(
    modifier: Modifier,
    batteryLevel: Int?,
    isBatteryCharging: Boolean,
    networkType: NetworkType?,
    signalStrength: NetworkSignalStrength?,
    speed: String?,
    elevation: String?
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        GroupMemberDeviceDataRowContent(
            batteryLevel = batteryLevel,
            isBatteryCharging = isBatteryCharging,
            networkType = networkType,
            signalStrength = signalStrength,
            speed = speed,
            elevation = elevation
        )
    }
}

@Composable
fun FlowRowScope.GroupMemberDeviceDataRowContent(
    state: GroupMemberItemLocationState?,
    isBatteryVisible: Boolean
) {
    GroupMemberDeviceDataRowContent(
        batteryLevel = state?.visibleBatteryLevel?.takeIf { isBatteryVisible },
        isBatteryCharging = state?.isBatteryCharging ?: false,
        networkType = state?.networkType,
        signalStrength = state?.signalStrength,
        speed = state?.speed,
        elevation = state?.elevation
    )
}

@Composable
fun FlowRowScope.GroupMemberDeviceDataRowContent(
    batteryLevel: Int?,
    isBatteryCharging: Boolean,
    networkType: NetworkType?,
    signalStrength: NetworkSignalStrength?,
    speed: String?,
    elevation: String?
) {
    val isNetworkVisible = networkType != null
    val isSpeedVisible = speed != null
    val isElevationVisible = elevation != null
    val isBatteryVisible = batteryLevel != null

    if (isNetworkVisible || isBatteryVisible) {
        Row(
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        ) {
            if (isBatteryVisible) {
                Row(
                    modifier = Modifier,
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
                            painter = if (isBatteryCharging) {
                                CoreAppIcons.BatteryChargingFull.asPainterResource()
                            } else {
                                CoreAppIcons.BatteryFull.asPainterResource()
                            },
                            tint = when {
                                batteryLevel > 85 -> AppColors.accent
                                batteryLevel < 15 -> AppColors.redMain
                                batteryLevel < 30 -> AppColors.highlightGold
                                else -> MaterialTheme.colors.onSurface
                            },
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "${batteryLevel}%",
                        style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium),
                        color = when {
                            batteryLevel < 15 -> AppColors.redMain
                            batteryLevel < 30 -> AppColors.highlightGold
                            else -> MaterialTheme.colors.onSurface
                        }
                    )
                }
            }

            if (isNetworkVisible) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NetworkTypeIcon(
                        networkType = networkType,
                        signalStrength = signalStrength
                    )
                    Text(
                        text = networkType.networkName,
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                }
            }
        }
    }

    if ((isBatteryVisible || isNetworkVisible) && (isSpeedVisible || isElevationVisible)) {
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .padding(horizontal = 8.dp)
                .requiredHeightIn(min = 12.dp)
                .width(0.5.dp)
                .background(color = AppColors.accent.copy(alpha = 0.2f))
        )
    }

    Row(
        modifier = Modifier.align(alignment = Alignment.CenterVertically),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
    ) {
        if (isSpeedVisible) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = CoreAppIcons.Speed.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(18.dp)
                )
                Text(
                    text = speed,
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
            }
        }

        if (isElevationVisible) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = CoreAppIcons.Elevation.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(18.dp)
                )
                Text(
                    text = elevation,
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
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
    startIconSize: Dp = 18.dp,
    endIcon: Painter? = CoreAppIcons.ChevronRight.asPainterResource(),
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    startIconColor: Color = contentColor,
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
                    modifier = Modifier.size(startIconSize),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Icon(
                    modifier = Modifier.size(startIconSize),
                    painter = startIcon,
                    contentDescription = null,
                    tint = startIconColor
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
    }

val MemberActionType.actionText: StringResource
    get() = when (this) {
        MemberActionType.DELETE_MEMBER -> MR.strings.group_members_item_remove_button
        MemberActionType.DELETE_LOCATION -> MR.strings.group_members_item_clear_location_button
        MemberActionType.BLOCK_USER -> MR.strings.group_details_block_group_alert_button
        MemberActionType.UNBLOCK_USER -> MR.strings.blocklist_unblock_alert_button
    }