package app.paralino.android.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.places.list.PlaceListItem
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.utils.extensions.fromUTCtoMapSharingTime
import app.paralino.core.MR
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.presentation.map.MapHomeScreenFocusType
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.presentation.map.MapHomeViewModel
import app.paralino.core.presentation.map.asFocusedMarker
import app.paralino.core.presentation.places.list.PlaceListItemData
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.groups.GroupPickerAction
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.core.utils.extensions.safeCast
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDateTime


@Composable
fun MapHomeScreenElements(
    modifier: Modifier = Modifier,
    appState: MainAppState,
    uiState: MapHomeScreenState,
    vm: MapHomeViewModel
) {
    val data by rememberUpdatedState(uiState)

    MapHomeScreenElements(
        modifier = modifier.fillMaxSize(),
        state = data,
        onGroupPickerClick = {
            appState.uiScope.launch {
                val groups = vm.getAllGroups()
                appState.showBottomSheetForResult(
                    BottomSheetsScreen.GroupPicker(groups)
                ).safeCast<GroupPickerAction>()?.let {
                    when (it) {
                        is GroupPickerAction.GroupPicked -> {
                            vm.switchGroup(it.group.id)
                        }

                        is GroupPickerAction.GroupEditPicked -> {
                            appState.navController.navigate(
                                AppScreen.MainHome.GroupDetails.route(it.group.id)
                            )
                        }

                        GroupPickerAction.NewGroup -> {
                            appState.navController.navigate(AppScreen.MainHome.GroupCreate.routeSignature)
                        }

                        GroupPickerAction.JoinGroup -> {
                            appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
                        }
                    }
                }
            }
        },
        onGroupDetailsClick = {
            when (val state = uiState) {
                is MapHomeScreenState.Ready -> {
                    appState.navController.navigate(AppScreen.MainHome.GroupDetails.route(state.groupId))
                }

                is MapHomeScreenState.Empty -> {}
            }
        },
        onSharingStartClick = {
            when (val state = uiState) {
                is MapHomeScreenState.Ready -> {
                    appState.navController.navigate(AppScreen.MainHome.SharingStart.route(state.groupId))
                }

                is MapHomeScreenState.Empty -> {}
            }
        },
        onSettingsClick = {
            appState.navController.navigate(AppScreen.Settings.routeSignature)
        },
        onMemberActionClick = { action, state ->
            vm.memberAction(action, state)
        },
        onMemberClick = {
            // TODO refocus right away
            vm.trackMarker(it.asFocusedMarker())
        },
        onPlaceClick = { state ->
            appState.navController.navigate(
                AppScreen.MainHome.PlaceDetail.route(
                    groupId = state.groupId,
                    placeId = state.id
                )
            )
        },
        onMapModeSwitch = {
            vm.switchMapMode()
        },
        onFocusOnPlacesClick = {
            vm.toggleFocusOnPlaces()
        },
        onFocusOnMembersClick = {
            vm.toggleFocusOnMembers()
        },
        onCloseFocusedItem = {
            vm.hideFocusedItem()
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
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
    onCloseFocusedItem: () -> Unit,
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
        val (groupSelect, placesTrack, membersTrack, focusedItem, shareTime, shareButton, settings, mapMode) = createRefs()
        val areMembersFocused = readyState?.mapHomeScreenFocusType?.areMembersFocused == true
        val arePlacesFocused = readyState?.mapHomeScreenFocusType?.arePlacesFocused == true
        val isMemberOrPlaceFocused =
            readyState?.mapHomeScreenFocusType?.isMemberOrPlaceFocused == true

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
        Box(
            modifier = Modifier.constrainAs(shareButton) {
                end.linkTo(parent.end, margin = 8.dp)
                bottom.linkTo(parent.bottom, margin = 144.dp)
            },
        ) {
            AnimatedVisibility(
                visible = readyState != null && readyState.sharingStatus == null && !isMemberOrPlaceFocused,
                enter = fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = {
                        Text(text = stringResource(id = MR.strings.groups_sharing_button_start.resourceId))
                    },
                    icon = {
                        Icon(painter = AppIcons.ShareLocation, contentDescription = null)
                    },
                    backgroundColor = MaterialTheme.colors.primary,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                    onClick = onSharingStartClick
                )
            }
        }

        // sharing time info
        AnimatedVisibility(
            visible = readyState?.sharingStatus != null && !isMemberOrPlaceFocused,
            modifier = Modifier.constrainAs(shareTime) {
                start.linkTo(parent.start, margin = 4.dp)
                bottom.linkTo(parent.bottom, margin = 174.dp)
            },
            enter = scaleIn(),
            exit = scaleOut() + fadeOut()
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
                        ?: stringResource(id = MR.strings.groups_sharing_duration_type_infinite.resourceId),
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp)
                )
            }
        }

        // member focus button
        AnimatedVisibility(
            modifier = Modifier
                .constrainAs(membersTrack) {
                    start.linkTo(placesTrack.start)
                    end.linkTo(placesTrack.end)
                    bottom.linkTo(placesTrack.top, margin = 12.dp)
                },
            visible = readyState != null && !isMemberOrPlaceFocused,
            enter = scaleIn(),
            exit = scaleOut() + fadeOut()
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

        // places focus button
        AnimatedVisibility(
            modifier = Modifier
                .constrainAs(placesTrack) {
                    end.linkTo(parent.end, margin = 8.dp)
                    bottom.linkTo(shareButton.top, margin = 16.dp)
                },
            visible = readyState != null && !isMemberOrPlaceFocused,
            enter = scaleIn(),
            exit = scaleOut() + fadeOut()
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

        // focused item
        Box(
            modifier = Modifier
                .constrainAs(focusedItem) {
                    bottom.linkTo(parent.bottom, 4.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .padding(horizontal = 4.dp)
        ) {
            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = readyState != null && readyState.mapHomeScreenFocusType is MapHomeScreenFocusType.PLACE,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                readyState?.focusedPlace?.let { placeState ->
                    Box {
                        PlaceListItem(
                            modifier = Modifier.padding(top = 20.dp),
                            state = placeState,
                            onClick = { onPlaceClick(placeState) }
                        )
                        CloseFocusButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 8.dp),
                            onClick = onCloseFocusedItem
                        )
                    }
                }
            }

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                visible = readyState != null && readyState.mapHomeScreenFocusType is MapHomeScreenFocusType.MEMBER,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                readyState?.focusedMember?.let { memberState ->
                    Box {
                        GroupMember(
                            modifier = Modifier.padding(top = 26.dp),
                            state = memberState,
                            backgroundColor = MaterialTheme.colors.background,
                            collapseOnActionClick = false,
                            forceExpandAction = true,
                            onActionClick = {
                                onMemberActionClick(it, memberState)
                            },
                            onClick = {
                                onMemberClick(memberState)
                            }
                        )
                        CloseFocusButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 8.dp),
                            onClick = onCloseFocusedItem
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloseFocusButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        modifier = modifier.size(36.dp),
        onClick = onClick,
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Icon(
            painter = CoreAppIcons.Close.asPainterResource(),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.secondary
        )
    }
}