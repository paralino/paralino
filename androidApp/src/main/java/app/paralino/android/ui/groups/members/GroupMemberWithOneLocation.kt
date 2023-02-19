package app.paralino.android.ui.groups.members

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.groups.GroupStatusTag
import app.paralino.android.ui.groups.GroupStatusTagType
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.android.utils.formatting.DistanceFormatter
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.GroupMemberStatus
import app.paralino.core.utils.Emojis

@Composable
fun ConstraintLayoutScope.GroupMemberWithOneLocation(
    state: GroupMemberItemStateWithOneLocation,
    forceExpandAction: Boolean = false,
    collapseOnActionClick: Boolean = true,
    onActionClick: (() -> Unit)? = null,
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
        if (state.location?.batterLevel != null) {
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
                        imageVector = AppIcons.BatteryFull,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "${state.location?.batterLevel}%",
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
        val iconId = "deviceIcon"
        Text(
            text = buildAnnotatedString {
                this.append(state.memberState.name)
                if (!state.location?.name.isNullOrBlank()) {
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
                        this.append(state.location?.name ?: "")
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
                            imageVector = AppIcons.PhoneAndroid,
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

        val status = when{
            !state.memberState.isVerified -> GroupStatusTagType.UNVERIFIED
            state.status == GroupMemberStatus.INACTIVE -> GroupStatusTagType.INACTIVE
            else -> null
        }
        GroupStatusTag(status)

        if (state.location?.lastPing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.AccessTime,
                    tint = MaterialTheme.colors.secondary,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(14.dp)
                )
                Text(
                    text = state.getLastPingText() ?: "",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.secondary,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {

            if (state.location?.speedMs != null) {
                Icon(
                    imageVector = AppIcons.Speed,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(18.dp)
                )
                Text(
                    text = DistanceFormatter.getSpeedSmall(
                        LocalContext.current,
                        state.location?.speedMs!!
                    ),
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }

            if (state.location?.elevationMeters != null) {
                Icon(
                    imageVector = AppIcons.StackedLineChart,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(18.dp)
                )
                Text(
                    text = DistanceFormatter.getDistanceSmall(
                        LocalContext.current,
                        state.location?.elevationMeters!!
                    ),
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
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
        if (state.memberState.actionItem != null || extraActions != null) {
            Icon(
                imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
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
        if (state.memberState.actionItem != null || extraActions != null) {
            AnimatedVisibility(visible = expanded) {
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