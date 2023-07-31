package app.paralino.core.ui.groups.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.MemberDeviceStatusType
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.GroupStatusTag
import app.paralino.core.ui.groups.GroupStatusTagType
import app.paralino.core.ui.groups.toStatusTagType
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.Emojis
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource


@Composable
fun GroupMemberWithLocations(
    state: GroupMemberItemStateWithLocations,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    onActionClick: ((MemberAction) -> Unit)? = null,
    onDeleteDeviceClick: ((GroupMemberItemLocationState) -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
) {

    var expanded by remember(state, forceExpandAction) { mutableStateOf(forceExpandAction) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // image
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
                    if (state.singleLocation?.batterLevel != null && (!expanded || !state.allowShowingDevices)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(90f),
                                painter = CoreAppIcons.BatteryFull.asPainterResource(),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "${state.singleLocation?.batterLevel}%",
                                style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium)
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

            // center content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val iconId = "deviceIcon"
                Text(
                    text = buildAnnotatedString {
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

                val status = when {
                    !state.memberState.isVerified -> GroupStatusTagType.UNVERIFIED
                    !expanded || state.locations.isEmpty() || !state.allowShowingDevices -> state.statusTag.toStatusTagType()
                    else -> null
                }
                GroupStatusTag(status)

                if (state.lastPing != null && (!expanded || !state.allowShowingDevices)) {
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
                            text = state.lastPing,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.singleLocation?.speed != null) {
                        Icon(
                            painter = CoreAppIcons.Speed.asPainterResource(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = state.singleLocation.speed,
                            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }

                    if (state.singleLocation?.elevation != null) {
                        Icon(
                            painter = CoreAppIcons.StackedLineChart.asPainterResource(),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = state.singleLocation.elevation,
                            style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }
            }

            val canExpand =
                state.memberState.actionItems.isNotEmpty() || extraActions != null || (state.locations.isNotEmpty() && state.allowShowingDevices)

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            AnimatedVisibility(visible = expanded) {
                Column {
                    state.locations.takeIf { state.allowShowingDevices }?.let {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            it.forEach {
                                GroupMemberLocationChild(
                                    state = it,
                                    onClick = {
                                        // open device
                                    },
                                    onDelete = {
                                        // delete device
                                        onDeleteDeviceClick?.invoke(it)
                                    }
                                )
                            }
                        }
                    }

                    if (state.memberState.actionItems.isNotEmpty() || extraActions != null) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.memberState.actionItems.forEach { action ->
                                FilledButton(
                                    style = ButtonStyle.SMALL_ACTION,
                                    icon = action.actionIcon,
                                    elevation = ButtonDefaults.elevation(pressedElevation = 2.dp),
                                    onClick = {
                                        if (collapseOnActionClick)
                                            expanded = !expanded
                                        onActionClick?.invoke(action)
                                    }
                                ) {
                                    Text(
                                        text = stringResource(action.actionText),
                                        style = MaterialTheme.typography.subtitle2.copy(fontSize = 10.sp)
                                    )
                                }
                                Spacer(modifier = Modifier.size(4.dp))
                            }

                            if (extraActions != null) {
                                extraActions()
                            }
                        }
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMemberLocationChild(
    modifier: Modifier = Modifier,
    state: GroupMemberItemLocationState,
    onClick: (GroupMemberItemLocationState) -> Unit,
    onDelete: (GroupMemberItemLocationState) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
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
                        if (state.currentDevice) 2.dp else 0.5.dp,
                        if (state.currentDevice)
                            MaterialTheme.colors.primary
                        else
                            MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(8.dp),
                painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                contentDescription = null
            )
            if (state.batterLevel != null) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(12.dp)
                                .rotate(90f),
                            painter = CoreAppIcons.BatteryFull.asPainterResource(),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            text = "${state.batterLevel}%",
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
                    bottom = if (state.batterLevel != null && state.lastPing == null) 12.dp else 0.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(MR.strings.group_members_device_name_default),
                style = MaterialTheme.typography.caption,
                modifier = Modifier
            )

            val status = when (state.status) {
                MemberDeviceStatusType.NOT_SHARING -> GroupStatusTagType.INACTIVE
                else -> state.status?.toStatusTagType()
            }
            GroupStatusTag(status)

            if (state.lastPing != null) {
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
                        text = state.lastPing,
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
                        painter = CoreAppIcons.StackedLineChart.asPainterResource(),
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

val MemberAction.actionIcon: Painter
    @Composable get() = when (this) {
        MemberAction.DELETE_MEMBER -> CoreAppIcons.PersonRemove.asPainterResource()
        MemberAction.DELETE_LOCATION -> CoreAppIcons.Delete.asPainterResource()
        MemberAction.BLOCK_USER -> CoreAppIcons.Block.asPainterResource()
        MemberAction.UNBLOCK_USER -> CoreAppIcons.LockOpen.asPainterResource()
    }

val MemberAction.actionText: StringResource
    get() = when (this) {
        MemberAction.DELETE_MEMBER -> MR.strings.group_members_item_remove_button
        MemberAction.DELETE_LOCATION -> MR.strings.group_members_item_clear_location_button
        MemberAction.BLOCK_USER -> MR.strings.group_details_block_group_alert_button
        MemberAction.UNBLOCK_USER -> MR.strings.blocklist_unblock_alert_button
    }