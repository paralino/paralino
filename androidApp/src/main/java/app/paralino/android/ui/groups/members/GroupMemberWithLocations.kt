package app.paralino.android.ui.groups.members

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.R
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.formatting.DistanceFormatter
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupMemberItemLocationState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.utils.Emojis


@Composable
fun ConstraintLayoutScope.GroupMemberWithLocations(
    state: GroupMemberItemStateWithLocations,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    onActionClick: (() -> Unit)? = null,
    onDeleteDeviceClick: ((GroupMemberItemLocationState) -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val (image, center, expand, actions) = createRefs()
    var expanded by remember(state, forceExpandAction) { mutableStateOf(forceExpandAction) }

    // image
    Column(modifier = Modifier
        .width(50.dp)
        .constrainAs(image) {
            start.linkTo(parent.start)
            top.linkTo(parent.top)
        }
    ) {
        ImageModelImage(
            imageModel = state.memberState.image ?: ImageModel.emoji(Emojis.robot),
            modifier = Modifier
                .size(50.dp)
                .border(
                    if (state.memberState.isCurrentUser) 2.dp else 0.5.dp,
                    if (state.memberState.isCurrentUser)
                        MaterialTheme.colors.primary
                    else
                        MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                    RoundedCornerShape(8.dp)
                )
        )
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    imageVector = Icons.Rounded.Devices,
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

    // center content
    Column(
        modifier = Modifier
            .constrainAs(center) {
                linkTo(
                    start = image.end,
                    end = expand.start,
                    top = parent.top,
                    bottom = actions.top,
                    startMargin = 12.dp,
                    endMargin = 8.dp,
                    horizontalBias = 0f
                )
                width = Dimension.fillToConstraints
            },
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = state.memberState.name,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
        )

        if (state.getLastPingText() != null) {
            Text(
                text = state.getLastPingText() ?: "",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.secondary,
            )
        }
    }

    // expand icon
    Box(
        modifier = Modifier.constrainAs(expand) {
            end.linkTo(parent.end)
            bottom.linkTo(image.bottom)
            top.linkTo(image.top)
        }
    ) {
        if (state.memberState.actionItem != null || extraActions != null || state.locations.isNotEmpty()) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null
            )
        }
    }

    // action item
    Column(modifier = Modifier
        .constrainAs(actions) {
            linkTo(
                start = parent.start,
                end = parent.end,
                top = image.bottom,
                bottom = parent.bottom,
                horizontalBias = 0f,
                verticalBias = 1f
            )
            width = Dimension.fillToConstraints
        }
    ) {

        AnimatedVisibility(visible = expanded) {
            Column {
                state.locations.forEach {
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

                if (state.memberState.actionItem != null || extraActions != null) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.memberState.actionItem != null) {
                            FilledButton(
                                style = ButtonStyle.SMALL_ACTION,
                                icon = state.memberState.actionItem!!.actionIcon,
                                elevation = ButtonDefaults.elevation(pressedElevation = 2.dp),
                                onClick = {
                                    if (collapseOnActionClick)
                                        expanded = !expanded
                                    onActionClick?.invoke()
                                }
                            ) {
                                Text(
                                    text = stringResource(id = state.memberState.actionItem!!.actionText),
                                    style = MaterialTheme.typography.subtitle2.copy(fontSize = 10.sp)
                                )
                            }
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
                onLongClick = { onDelete(state) }
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.width(36.dp),
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
                imageVector = Icons.Rounded.Smartphone,
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
                            imageVector = Icons.Rounded.BatteryFull,
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
                .padding(start = 8.dp, end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(id = R.string.group_members_device_name_default),
                style = MaterialTheme.typography.caption,
                modifier = Modifier
            )

            if (state.status.actionText != null) {
                Text(
                    text = stringResource(id = state.status.actionText!!),
                    style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.secondary,
                            MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }

            if (state.lastPing != null) {
                Text(
                    text = state.getLastPingText() ?: "",
                    style = MaterialTheme.typography.overline,
                    color = MaterialTheme.colors.secondary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {

                if (state.speedMs != null) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = DistanceFormatter.getSpeedSmall(
                            LocalContext.current,
                            state.speedMs!!
                        ),
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                if (state.elevationMeters != null) {
                    Icon(
                        imageVector = Icons.Rounded.StackedLineChart,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = DistanceFormatter.getDistanceSmall(
                            LocalContext.current,
                            state.elevationMeters!!
                        ),
                        style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }

        }
    }
}