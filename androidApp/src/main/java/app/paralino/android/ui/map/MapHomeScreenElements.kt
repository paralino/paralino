package app.paralino.android.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.paralino.android.R
import app.paralino.android.ui.places.list.PlaceListItem
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.utils.extensions.fromUTCtoMapSharingTime
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.GroupMemberItemStateWithLocations
import app.paralino.core.presentation.groups.GroupMemberItemStateWithOneLocation
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.presentation.map.FocusedMarkerData
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapPinType
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.isoToLocalDateTime
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.datetime.toJavaLocalDateTime


@OptIn(ExperimentalPagerApi::class, ExperimentalAnimationApi::class)
@Composable
fun MapHomeScreenElements(
    modifier: Modifier = Modifier,
    state: MapHomeScreenState,
    onGroupPickerClick: () -> Unit,
    onGroupDetailsClick: () -> Unit,
    onFocusOnMembersClick: () -> Unit,
    onFocusOnPlacesClick: () -> Unit,
    onSharingStartClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMemberActionClick: (MemberAction, GroupMemberItemState) -> Unit,
    onMemberClick: (GroupMemberItemState) -> Unit,
    onPlaceClick: (PlaceListItemData) -> Unit,
    onMapModeSwitch: () -> Unit,
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

        val readyState = state as? MapHomeScreenState.Ready
        val (groupSelect, groupTrack, userListButton, userList, shareTime, settings, mapMode) = createRefs()
        val areMembersFocused = readyState?.mapHomeScreenFocusType?.areMembersFocused == true
        val arePlacesFocused = readyState?.mapHomeScreenFocusType?.arePlacesFocused == true
        val isUserListExpanded by remember(readyState?.focusedMarker, areMembersFocused, readyState?.mapHomeScreenFocusType?.areMembersExpanded) {
            mutableStateOf(areMembersFocused || readyState?.mapHomeScreenFocusType?.areMembersExpanded == true || readyState?.focusedMarker?.type == MapPinType.MEMBER)
        }
        val isPlacesListExpanded by remember(readyState?.focusedMarker, arePlacesFocused, readyState?.mapHomeScreenFocusType?.arePlacesExpanded) {
            mutableStateOf(arePlacesFocused || readyState?.mapHomeScreenFocusType?.arePlacesExpanded == true || readyState?.focusedMarker?.type == MapPinType.PLACE)
        }

        FloatingActionButton(
            modifier = Modifier
                .constrainAs(settings) {
                    end.linkTo(parent.end, margin = 12.dp)
                    top.linkTo(groupSelect.top)
                    bottom.linkTo(groupSelect.bottom)
                }
                .size(46.dp),
            onClick = onSettingsClick,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            ImageModelImage(
                imageModel = state.currentUserImage,
                useBackgroundForEmoji = false,
                clipShape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = CircleShape
                    )
            )
        }

        // group picker button
        AnimatedVisibility(
            modifier = Modifier
                .constrainAs(groupSelect) {
                    top.linkTo(parent.top, margin = 8.dp)
                    start.linkTo(parent.start, margin = 12.dp)
                    end.linkTo(settings.start, margin = 24.dp)
                    width = Dimension.fillToConstraints
                },
            visible = state is MapHomeScreenState.Ready,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            if (readyState != null) {
                ConstraintLayout(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val groupPicker = createRef()
                    MapGroupPicker(
                        modifier = Modifier.constrainAs(groupPicker) {
                            linkTo(
                                top = parent.top,
                                bottom = parent.bottom,
                                start = parent.start,
                                end = parent.end,
                                horizontalBias = 0f
                            )
                            width = Dimension.fillToConstraints
                        },
                        groupName = readyState.groupName,
                        groupImage = readyState.groupImage,
                        userCount = readyState.userCount,
                        inactiveUserCount = readyState.inactiveUserCount,
                        activeUserCount = readyState.activeUserCount,
                        idleUserCount = readyState.idleUserCount,
                        verified = readyState.allMembersVerified,
                        onGroupClick = onGroupDetailsClick,
                        onExpandClick = onGroupPickerClick
                    )
                }
            }
        }

        // map mode
        Image(
            painter = if (!state.isSatelliteMode) {
                CoreAppIcons.MapModeSatellite.asPainterResource()
            } else if (isSystemInDarkTheme()) {
                CoreAppIcons.MapModeDefaultDark.asPainterResource()
            } else {
                CoreAppIcons.MapModeDefaultLight.asPainterResource()
            },
            contentDescription = null,
            modifier = Modifier
                .constrainAs(mapMode) {
                    end.linkTo(parent.end, margin = 12.dp)
                    top.linkTo(settings.bottom, margin = 20.dp)
                }
                .size(40.dp)
                .background(
                    color = AppColors.accentDark,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onMapModeSwitch() },
            contentScale = ContentScale.Crop
        )

        // sharing start
        AnimatedVisibility(
            visible = readyState != null && readyState.sharingStatus == null,
            modifier = Modifier
                .constrainAs(shareTime) {
                    start.linkTo(parent.start, margin = 16.dp)
                    bottom.linkTo(groupTrack.bottom)
                },
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            ExtendedFloatingActionButton(
                text = {
                    Text(text = stringResource(id = R.string.groups_sharing_button_start))
                },
                icon = {
                    Icon(painter = AppIcons.ShareLocation, contentDescription = null)
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                onClick = onSharingStartClick
            )
        }

        // sharing time info
        AnimatedVisibility(
            visible = readyState?.sharingStatus != null,
            modifier = Modifier
                .constrainAs(shareTime) {
                    start.linkTo(parent.start, margin = 16.dp)
                    bottom.linkTo(groupTrack.bottom)
                },
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colors.surface,
                        MaterialTheme.shapes.medium
                    )
                    .clip(RoundedCornerShape(percent = 100))
                    .clickable { onGroupDetailsClick() }
                    .padding(vertical = 4.dp)
                    .padding(start = 4.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = AppIcons.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = readyState?.sharingStatus?.endTimeUTC
                        ?.isoToLocalDateTime()
                        ?.toJavaLocalDateTime()
                        ?.fromUTCtoMapSharingTime(LocalContext.current)
                        ?: stringResource(id = R.string.groups_sharing_duration_type_infinite),
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp)
                )
            }
        }

        // expand member list button
        AnimatedVisibility(
            modifier = Modifier
                .constrainAs(userListButton) {
                    start.linkTo(groupTrack.start)
                    end.linkTo(groupTrack.end)
                    bottom.linkTo(groupTrack.top, margin = 16.dp)
                },
            visible = readyState != null,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            FloatingActionButton(
                modifier = Modifier.size(44.dp),
                onClick = onFocusOnMembersClick,
                backgroundColor = if (areMembersFocused) AppColors.accent else MaterialTheme.colors.surface
            ) {
                Icon(
                    painter = AppIcons.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (areMembersFocused) AppColors.darkThemePrimary else MaterialTheme.colors.secondary
                )
            }
        }

        // activate group tracking button
        AnimatedVisibility(
            modifier = Modifier
                .constrainAs(groupTrack) {
                    end.linkTo(parent.end, margin = 8.dp)
                    bottom.linkTo(userList.top, margin = 16.dp)
                },
            visible = readyState != null,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            FloatingActionButton(
                modifier = Modifier.size(44.dp),
                onClick = onFocusOnPlacesClick,
                backgroundColor = if (arePlacesFocused) AppColors.accent else MaterialTheme.colors.surface
            ) {
                Icon(
                    painter = CoreAppIcons.MapsHomeWork.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (arePlacesFocused) AppColors.darkThemePrimary else MaterialTheme.colors.secondary
                )
            }
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
                visible = isPlacesListExpanded && readyState != null,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if(readyState != null) {
                    val pagerState = rememberPagerState()
                    HorizontalPager(
                        state = pagerState,
                        itemSpacing = 4.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        count = readyState.places.size,
                        key = { readyState.places.get(it).id }
                    ) { index ->
                        PlaceListItem(
                            state = readyState.places.get(index),
                            onClick = { onPlaceClick(readyState.places.get(index)) }
                        )
                    }
                    LaunchedEffect(readyState.focusedMarker) {
                        val page = readyState.places.indexOfFirst {
                            it.id == readyState.focusedMarker?.markerId
                        }.takeIf { it > 0 } ?: 0
                        pagerState.animateScrollToPage(page)
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = isUserListExpanded && readyState != null,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (readyState != null) {
                    val pagerState = rememberPagerState()
                    HorizontalPager(
                        state = pagerState,
                        itemSpacing = 4.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        count = readyState.members.size,
                        key = { readyState.members.get(it).keyId }
                    ) {
                        val memberState = readyState.members.get(it)
                        var expanded by remember {
                            mutableStateOf(false)
                        }
                        GroupMember(
                            state = memberState,
                            backgroundColor = MaterialTheme.colors.background,
                            collapseOnActionClick = true,
                            forceExpandAction = expanded,
                            onActionClick = {
                                onMemberActionClick(it, memberState)
                            },
                            onClick = {
                                expanded = !expanded
                                onMemberClick(memberState)
                            }
                        )
                    }
                    LaunchedEffect(readyState.focusedMarker) {
                        val page = readyState.members.indexOfFirst {
                            when (it) {
                                is GroupMemberItemStateWithLocations -> false
                                is GroupMemberItemStateWithOneLocation -> {
                                    it.keyId == readyState.focusedMarker?.markerId
                                }
                            }
                        }.takeIf { it > 0 } ?: 0
                        pagerState.animateScrollToPage(page)
                    }
                }
            }
        }
    }
}