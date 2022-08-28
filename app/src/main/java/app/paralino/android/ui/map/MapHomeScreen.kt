package app.paralino.android.ui.map

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.GroupWork
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.dashboard.GroupItemData
import app.paralino.core.presentation.groups.GroupMemberItemState
import app.paralino.core.presentation.map.MapHomeScreenState
import app.paralino.core.utils.extensions.isoToLocalDateTime
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.BottomSheetsScreen
import app.paralino.android.ui.groups.members.GroupMember
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.utils.extensions.fromUTCtoMapSharingTime
import app.paralino.android.utils.extensions.safeCast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDateTime
import org.koin.androidx.compose.getViewModel

@Composable
fun MapHomeScreen(
    appState: MainAppState,
    groupId: String?,
    vm: MapViewModel = getViewModel(),
) {
    val mapViewState = rememberMapViewProviderState(darkMode = isSystemInDarkTheme())

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
    rememberSaveable(groupId, isSystemInDarkTheme()) {
        vm.switchGroup(groupId = groupId ?: "")
        ""
    }

    var trackGroup by rememberSaveable(isSystemInDarkTheme()) { mutableStateOf(true) }
    var trackMarkerId: String? by rememberSaveable(isSystemInDarkTheme()) { mutableStateOf(null) }

    LaunchedEffect(uiState.data?.groupId) {
        vm.locationRefreshSubscription.collect()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapViewState.initMap()
                mapViewState.onMapTouch = {
                    trackGroup = false
                    trackMarkerId = null
                    false
                }
                mapViewState.onMarkerClick = {
                    trackMarkerId = it
                    trackGroup = false
                    true
                }

                mapViewState.mapAndroidView
            },
            update = { }
        )

        when (val state = uiState) {
            is Payload.Error -> {}
            Payload.Loading -> {}
            is Payload.Success -> {

                val data by rememberUpdatedState(state.data)
                val markers by rememberUpdatedState(data.markers)

                LaunchedEffect(markers) {
                    mapViewState.updateMarkers(list = markers)
                }

                LaunchedEffect(data, trackGroup) {
                    mapViewState.reDrawMarkers(trackMarkerId, trackGroup)
                }

                @Suppress("USELESS_IS_CHECK")
                AnimatedVisibility(
                    visible = state is Payload.Success,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    MapHomeScreenElements(
                        modifier = Modifier.fillMaxSize(),
                        state = data,
                        isTrackingGroup = trackGroup,
                        trackingUserId = trackMarkerId,
                        onGroupPickerClick = {
                            appState.uiScope.launch {
                                val groups = vm.getAllGroups()
                                appState.showBottomSheetForResult(
                                    BottomSheetsScreen.GroupPicker(groups)
                                ).safeCast<GroupItemData>()?.let {
                                    vm.switchGroup(it.id)
                                }
                            }
                        },
                        onTrackGroupClick = {
                            trackGroup = !trackGroup
                            trackMarkerId = null
                        },
                        onClearMyLocationClick = {
                            vm.clearMyLocation()
                        },
                        onMemberClick = {
                            trackGroup = false
                            trackMarkerId = it.id
                            mapViewState.focusToMarker(it.id)
                        }
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MapHomeScreenElements(
    modifier: Modifier = Modifier,
    state: MapHomeScreenState,
    isTrackingGroup: Boolean,
    trackingUserId: String?,
    onGroupPickerClick: () -> Unit,
    onTrackGroupClick: () -> Unit,
    onClearMyLocationClick: () -> Unit,
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

        val (groupSelect, groupTrack, userListButton, userList, shareTime) = createRefs()
        var userListExpanded by remember(trackingUserId) { mutableStateOf(!trackingUserId.isNullOrBlank()) }

        // group picker button
        MapGroupPicker(
            modifier = Modifier
                .constrainAs(groupSelect) {
                    top.linkTo(parent.top, margin = 8.dp)
                    centerHorizontallyTo(parent)
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
            inactiveUserCount = state.inactiveUserCount
        ) {
            onGroupPickerClick()
        }

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
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = state.sharingStatus!!.endTimeUTC
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
                imageVector = Icons.Rounded.GroupWork,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isTrackingGroup) AppColors.darkThemePrimary else MaterialTheme.colors.secondary
            )
        }

        snapshotFlow { }

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
                    key = { state.members.get(it).id }
                ) {
                    val memberState = state.members[it]
                    GroupMember(
                        state = memberState,
                        backgroundColor = MaterialTheme.colors.background,
                        onActionClick = {
                            onClearMyLocationClick()
                        },
                        onClick = { onMemberClick(memberState) }
                    )
                }
                LaunchedEffect(trackingUserId) {
                    val page =
                        state.members.indexOfFirst { it.id == trackingUserId }.takeIf { it > 0 }
                            ?: 0
                    pagerState.animateScrollToPage(page)
                }
            }
        }
    }
}