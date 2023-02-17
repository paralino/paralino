package app.paralino.android.ui.map

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.R
import app.paralino.android.ui.groups.members.GroupMember
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.utils.extensions.fromUTCtoMapSharingTime
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.utils.extensions.isoToLocalDateTime
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.datetime.toJavaLocalDateTime


@OptIn(ExperimentalPagerApi::class)
@Composable
fun MapHomeScreenElements(
    modifier: Modifier = Modifier,
    state: MapHomeScreenState,
    isTrackingGroup: Boolean,
    focusedMarker: FocusedMarkerData?,
    onGroupPickerClick: () -> Unit,
    onGroupDetailsClick: () -> Unit,
    onTrackGroupClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onClearMyLocationClick: (String) -> Unit,
    onMemberClick: (GroupMemberItemState) -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {

        val (groupSelect, groupTrack, userListButton, userList, shareTime, settings, groupDetails) = createRefs()
        var userListExpanded by remember(focusedMarker) { mutableStateOf(focusedMarker?.type == MapPinType.MEMBER) }

        FloatingActionButton(
            modifier = Modifier
                .constrainAs(settings) {
                    end.linkTo(parent.end, margin = 8.dp)
                    top.linkTo(groupSelect.top)
                    bottom.linkTo(groupSelect.bottom)
                }
                .size(36.dp),
            onClick = onSettingsClick,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colors.onSurface
            )
        }

        // group picker button
        MapGroupPicker(
            modifier = Modifier
                .constrainAs(groupSelect) {
                    top.linkTo(parent.top, margin = 8.dp)
                    start.linkTo(parent.start, margin = 36.dp)
                    end.linkTo(settings.start)
                }
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            groupName = state.groupName,
            groupImage = state.groupImage,
            userCount = state.userCount,
            inactiveUserCount = state.inactiveUserCount,
            onGroupClick = onGroupDetailsClick,
            onExpandClick = onGroupPickerClick
        )

        // sharing time info
        AnimatedVisibility(
            visible = state.sharingStatus != null,
            modifier = Modifier
                .constrainAs(shareTime) {
                    start.linkTo(parent.start, margin = 16.dp)
                    bottom.linkTo(groupTrack.bottom)
                }
        ) {
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colors.surface,
                        MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 4.dp)
                    .padding(start = 4.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = state.sharingStatus?.endTimeUTC
                        ?.isoToLocalDateTime()
                        ?.toJavaLocalDateTime()
                        ?.fromUTCtoMapSharingTime(LocalContext.current)
                        ?: stringResource(id = R.string.groups_sharing_duration_type_infinite),
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp)
                )
            }
        }

        // expand member list button
        FloatingActionButton(
            modifier = Modifier
                .constrainAs(userListButton) {
                    start.linkTo(groupTrack.start)
                    end.linkTo(groupTrack.end)
                    bottom.linkTo(groupTrack.top, margin = 16.dp)
                }
                .size(40.dp),
            onClick = { userListExpanded = !userListExpanded },
            backgroundColor = if (userListExpanded) AppColors.accent else MaterialTheme.colors.surface
        ) {
            Icon(
                imageVector = Icons.Rounded.Groups,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (userListExpanded) AppColors.darkThemePrimary else MaterialTheme.colors.secondary
            )
        }

        // activate group tracking button
        FloatingActionButton(
            modifier = Modifier
                .constrainAs(groupTrack) {
                    end.linkTo(parent.end, margin = 8.dp)
                    bottom.linkTo(userList.top, margin = 16.dp)
                }
                .size(50.dp),
            onClick = onTrackGroupClick,
            backgroundColor = if (isTrackingGroup) AppColors.accent else MaterialTheme.colors.surface
        ) {
            Icon(
                imageVector = Icons.Rounded.CenterFocusStrong,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isTrackingGroup) AppColors.darkThemePrimary else MaterialTheme.colors.secondary
            )
        }

        // user list
        Box(
            modifier = Modifier.constrainAs(userList) {
                bottom.linkTo(parent.bottom, 14.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        ) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = userListExpanded,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val pagerState = rememberPagerState()
                HorizontalPager(
                    state = pagerState,
                    itemSpacing = 4.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    count = state.members.size,
                    key = { state.members.get(it).keyId }
                ) {
                    val memberState = state.members[it]
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    GroupMember(
                        state = memberState,
                        backgroundColor = MaterialTheme.colors.background,
                        collapseOnActionClick = true,
                        forceExpandAction = expanded,
                        onActionClick = {
                            if (memberState is GroupMemberItemStateWithOneLocation && memberState.location != null) {
                                onClearMyLocationClick(memberState.location!!.locationId)
                            }
                        },
                        onClick = {
                            expanded = !expanded
                            onMemberClick(memberState)
                        }
                    )
                }
                LaunchedEffect(focusedMarker) {
                    val page = state.members.indexOfFirst {
                        when (it) {
                            is GroupMemberItemStateWithLocations -> false
                            is GroupMemberItemStateWithOneLocation -> {
                                it.keyId == focusedMarker?.markerId
                            }
                        }
                    }.takeIf { it > 0 } ?: 0
                    pagerState.animateScrollToPage(page)
                }
            }
        }
    }
}