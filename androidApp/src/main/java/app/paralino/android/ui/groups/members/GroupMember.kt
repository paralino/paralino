package app.paralino.android.ui.groups.members

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.R
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.theme.AppTheme
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.extensions.fromUTCtoUserLastPing
import app.paralino.android.utils.formatting.DistanceFormatter
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberStatus
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.utils.Emojis
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

@Composable
fun GroupMember(
    state: GroupMemberItemState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    onActionClick: (() -> Unit)? = null,
    extraActions: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
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
                imageModel = state.image ?: ImageModel.emoji(Emojis.robot),
                modifier = Modifier
                    .size(50.dp)
                    .border(
                        0.5.dp,
                        MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                        RoundedCornerShape(8.dp)
                    )
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
                                .size(14.dp)
                                .rotate(90f),
                            imageVector = Icons.Rounded.BatteryFull,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "${state.batterLevel}%",
                            style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Medium)
                        )
                    }
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
                text = state.name,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
            )

            if (state.status.actionText != null) {
                Text(
                    text = stringResource(id = state.status.actionText!!),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
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
                    style = MaterialTheme.typography.caption,
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

        // expand icon
        if (state.actionItem != null || extraActions != null) {
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.constrainAs(expand) {
                    end.linkTo(parent.end)
                    bottom.linkTo(image.bottom)
                    top.linkTo(image.top)
                }
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }
        } else {
            Spacer(
                modifier = Modifier
                    .size(1.dp)
                    .constrainAs(expand) {
                        end.linkTo(parent.end)
                        bottom.linkTo(image.bottom)
                        top.linkTo(image.top)
                    }
            )
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
            if (state.actionItem != null || extraActions != null) {
                AnimatedVisibility(visible = expanded) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.actionItem != null) {
                            FilledButton(
                                style = ButtonStyle.SMALL_ACTION,
                                icon = state.actionItem!!.actionIcon,
                                elevation = ButtonDefaults.elevation(pressedElevation = 2.dp),
                                onClick = {
                                    if (collapseOnActionClick)
                                        expanded = !expanded
                                    onActionClick?.invoke()
                                }
                            ) {
                                Text(
                                    text = stringResource(id = state.actionItem!!.actionText),
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

@Composable
fun GroupMemberItemState.getLastPingText(): String? {
    return lastPing?.toJavaLocalDateTime()?.let {
        if (it.isAfter(LocalDateTime.now().minusMinutes(2)))
            stringResource(R.string.group_members_last_active_status_now)
        else
            it.fromUTCtoUserLastPing()
    }
}

val MemberAction.actionIcon: ImageVector
    get() = when (this) {
        MemberAction.DELETE_MEMBER -> Icons.Rounded.PersonRemove
        MemberAction.DELETE_LOCATION -> Icons.Rounded.Delete
    }

val MemberAction.actionText: Int
    get() = when (this) {
        MemberAction.DELETE_MEMBER -> R.string.group_members_item_remove_button
        MemberAction.DELETE_LOCATION -> R.string.group_members_item_clear_location_button
    }

val GroupMemberStatus.actionText: Int?
    get() = when (this) {
        GroupMemberStatus.ACTIVE -> null
        GroupMemberStatus.INACTIVE -> R.string.group_members_status_inactive
    }

@Preview(device = Devices.PIXEL_3A)
@Composable
fun MemberPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column(modifier = Modifier.padding(40.dp)) {
                GroupMember(state = dummyMemberState) { }
            }
        }
    }
}

val dummyMemberState = GroupMemberItemState(
    locationId = "",
    memberId = "",
    userId = "",
    actionItem = MemberAction.DELETE_MEMBER,
    name = "User name",
    image = ImageModel.emoji(Emojis.robot),
    batterLevel = 20,
    speedMs = 10f,
    elevationMeters = 50f,
    lastPing = LocalDateTime.now().minusDays(2).toKotlinLocalDateTime(),
    status = GroupMemberStatus.INACTIVE
)