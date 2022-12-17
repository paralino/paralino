package app.paralino.android.ui.groups.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.android.R
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.*
import app.paralino.android.ui.groups.members.GroupMember
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.settings.SettingsSectionHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppColors
import app.paralino.android.utils.extensions.annotatedBold
import app.paralino.android.utils.extensions.fromUTCtoMapSharingTime
import app.paralino.android.utils.extensions.toLocalDateTime
import app.paralino.android.utils.formatting.DistanceFormatter
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.location.LocationSharingContract
import app.paralino.core.presentation.groups.*
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String?,
) {
    if (groupId.isNullOrBlank()) {
        DefaultEmptyState(content = stringResource(id = R.string.group_load_error))
    } else {
        GroupDetailScreen(appState = appState, groupId = groupId)
    }
}

@Composable
fun GroupDetailScreen(
    appState: MainAppState,
    groupId: String,
    vm: GroupDetailViewModel = getViewModel { parametersOf(groupId) },
) {

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is GroupDetailScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                GroupDetailScreenEvent.GroupNotFound -> {
                    appState.showSnackbar(context.getString(R.string.group_load_error))
                }
                GroupDetailScreenEvent.GroupDeleted -> {
                    appState.navController.popBackStack()
                }
                GroupDetailScreenEvent.GroupMemberDeleted -> {}
                GroupDetailScreenEvent.MemberLocationDeleted -> {}
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val state = uiState) {
        Payload.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
        is Payload.Error -> {
            DefaultEmptyState(content = stringResource(id = R.string.group_load_error))
        }
        is Payload.Success -> {

            val locationPermissionHandler = rememberLocationPermissionHandler(
                appState = appState,
                locationStatusProvider = vm.locationStatusProvider
            )

            val notificationPermissionHandler = rememberNotificationsPermissionHandler(
                appState = appState,
                locationStatusProvider = vm.locationStatusProvider
            )

            when (val dialog = showDialog) {
                is MainAlertDialog.DeleteGroup -> {
                    dialog.Show {
                        showDialog = null
                        if (it == MainAlertDialog.Buttons.buttonOk)
                            vm.leaveOrDeleteGroup()
                    }
                }
                is MainAlertDialog.LeaveGroup -> {
                    dialog.Show {
                        showDialog = null
                        if (it == MainAlertDialog.Buttons.buttonOk)
                            vm.leaveOrDeleteGroup()
                    }
                }
                is MainAlertDialog.DeleteGroupMember -> {
                    dialog.Show {
                        showDialog = null
                        if (it == MainAlertDialog.Buttons.buttonOk)
                            vm.removeMember(dialog.memberUserId)
                    }
                }
                is MainAlertDialog.DeleteMemberLocation -> {
                    dialog.Show {
                        showDialog = null
                        if (it == MainAlertDialog.Buttons.buttonOk)
                            vm.removeMemberLocation(
                                memberUserId = dialog.memberId,
                                locationId = dialog.locationId
                            )
                    }
                }
                else -> {}
            }

            GroupDetailScreen(
                state = state.data,
                onScreenClose = {
                    appState.navController.popBackStack()
                },
                onGroupEdit = {
                    appState.navController.navigate(
                        AppScreen.DashboardRoot.GroupCreate.route(groupId)
                    )
                },
                onAddGroupMember = {
                    appState.navController.navigate(
                        AppScreen.DashboardRoot.GroupMemberAdd.route(groupId = groupId)
                    )
                },
                onGroupDeleteOrLeave = {
                    if (state.data.canEdit) {
                        showDialog = MainAlertDialog.DeleteGroup
                    } else {
                        showDialog = MainAlertDialog.LeaveGroup
                    }
                },
                onMemberDelete = {
                    showDialog = MainAlertDialog.DeleteGroupMember(it.memberState.userId, it.memberState.name)
                },
                onMemberDeviceDelete = { member, location ->
                    showDialog = MainAlertDialog.DeleteMemberLocation(
                        memberId = member.memberState.memberId,
                        locationId = location.locationId,
                        isUserOwnDevice = member.memberState.isCurrentUser,
                        memberName = member.memberState.name
                    )
                },
                onOpenMap = {
                    appState.navController.navigate(
                        AppScreen.MapRoot.MainMap.route(groupId)
                    )
                },
                openPlaces = {
                    appState.navController.navigate(
                        AppScreen.DashboardRoot.PlacesList.route(groupId)
                    )
                },
                openRawDataPresentation = {
                    appState.navController.navigate(
                        AppScreen.DashboardRoot.RawDataScreen.route(
                            RawPresentationDataType.GroupPresentation(groupId)
                        )
                    )
                },
                openMemberRawDataPresentation = {
                    appState.navController.navigate(
                        AppScreen.DashboardRoot.RawDataScreen.route(
                            RawPresentationDataType.GroupMemberPresentation(it)
                        )
                    )
                },
                onSharingAction = {
                    if (state.data.sharingStatus != null) {
                        vm.stopSharing()
                    } else {
                        appState.navController.navigate(
                            AppScreen.DashboardRoot.SharingStart.route(groupId)
                        )
                    }
                },
                onErrorAction = {
                    when (it) {
                        ConditionErrorType.GPS -> locationPermissionHandler.askLocationService()
                        ConditionErrorType.PERMISSION_NOTIFICATIONS -> notificationPermissionHandler.askNotificationPermission()
                        ConditionErrorType.PERMISSION_LOCATION -> locationPermissionHandler.askLocationPermission()
                        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> locationPermissionHandler.askBackgroundLocationPermission()
                        else -> { /* TODO review or do nothing */
                        }
                    }
                }
            )
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreen(
    state: GroupDetailScreenState,
    onScreenClose: () -> Unit,
    onGroupEdit: () -> Unit,
    onAddGroupMember: () -> Unit,
    onGroupDeleteOrLeave: () -> Unit,
    onMemberDelete: (GroupMemberItemState) -> Unit,
    onMemberDeviceDelete: (GroupMemberItemState, GroupMemberItemLocationState) -> Unit,
    onOpenMap: () -> Unit,
    openPlaces: () -> Unit,
    openRawDataPresentation: () -> Unit,
    openMemberRawDataPresentation: (memberId: String) -> Unit,
    onSharingAction: () -> Unit,
    onErrorAction: (ConditionErrorType) -> Unit,
) {

    var optionMenuExpanded by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { },
                navigationIcon = {
                    IconButton(onClick = onScreenClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            openRawDataPresentation()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            tint = AppColors.accent,
                            contentDescription = null
                        )
                    }

                    if (state.canEdit) {
                        IconButton(onClick = onAddGroupMember) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = onGroupEdit) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null
                            )
                        }
                    }

                    IconButton(onClick = { optionMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = optionMenuExpanded,
                        onDismissRequest = { optionMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                optionMenuExpanded = false
                                onGroupDeleteOrLeave()
                            }
                        ) {
                            Text(
                                text = if (state.canEdit) stringResource(id = R.string.group_details_menu_delete)
                                else stringResource(id = R.string.group_details_menu_leave)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    if (state.sharingStatus != null) {
                        Text(text = stringResource(id = R.string.location_banner_info_stop))
                    } else {
                        Text(text = stringResource(id = R.string.groups_sharing_button_start))
                    }
                },
                icon = {
                    if (state.sharingStatus != null) {
                        Icon(imageVector = Icons.Rounded.StopCircle, contentDescription = null)
                    } else {
                        Icon(imageVector = Icons.Rounded.ShareLocation, contentDescription = null)
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                onClick = onSharingAction
            )
        },
        isFloatingActionButtonDocked = true
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = it.calculateTopPadding() + 20.dp,
                bottom = it.calculateBottomPadding() + 100.dp
            )
        ) {

            this.items(items = state.errors, key = { "error_$it" }) {
                ConditionErrorItem(data = it) { onErrorAction(it) }
            }

            if (state.errors.isNotEmpty())
                this.item { Spacer(modifier = Modifier.size(10.dp)) }

            if (state.sharingStatus != null) {
                this.item(key = "sharing_status") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        GroupDetailSharingStatusElement(
                            contract = state.sharingStatus!!
                        )
                    }
                }
                this.item { Spacer(modifier = Modifier.size(30.dp)) }
            }

            this.item(key = "main_header") {
                GroupDetailsHeader(
                    groupName = state.name,
                    groupImage = state.image,
                    onMapClick = onOpenMap,
                    onPlacesClick = openPlaces
                )
            }

            this.stickyHeader(key = "member_list_header") {
                SettingsSectionHeader(
                    name = stringResource(
                        id = R.string.group_details_section_members_title,
                        state.members.size
                    )
                )
                Spacer(modifier = Modifier.size(4.dp))
            }

            this.itemsIndexed(
                items = state.members,
                key = { index, item -> item.memberState.memberId }
            ) { index, item ->

                if (index > 0)
                    Spacer(modifier = Modifier.size(8.dp))

                var expanded by remember {
                    mutableStateOf(false)
                }

                GroupMember(
                    modifier = Modifier.animateItemPlacement(),
                    state = item,
                    forceExpandAction = expanded,
                    collapseOnActionClick = false,
                    onClick = { expanded = !expanded },
                    onActionClick = { onMemberDelete(item) },
                    onDeleteDeviceClick = {
                        onMemberDeviceDelete(item, it)
                    },
                    extraActions = {
                        IconButton(
                            onClick = {
                                openMemberRawDataPresentation(item.memberState.memberId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                tint = AppColors.accent,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }

    AnimatedVisibility(state.loadingAction, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background.copy(alpha = 0.5f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

}

@Composable
fun GroupDetailsHeader(
    modifier: Modifier = Modifier,
    groupName: String,
    groupImage: ImageModel,
    onMapClick: () -> Unit,
    onPlacesClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        ImageModelImage(
            modifier = Modifier.size(140.dp),
            imageModel = groupImage
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = groupName,
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.size(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LargeSeekbarSwitch(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .fillMaxWidth(0.5f),
                icon = Icons.Rounded.Map,
                title = stringResource(id = R.string.title_map),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIcon = Icons.Rounded.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onMapClick()
                }
            )
            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.MapsHomeWork,
                title = stringResource(id = R.string.places_list_screen_title),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIcon = Icons.Rounded.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onPlacesClick()
                }
            )
        }

        Spacer(modifier = Modifier.size(20.dp))
    }
}

@Composable
fun GroupDetailSharingStatusElement(
    modifier: Modifier = Modifier,
    contract: LocationSharingContract,
) {
    val backgroundColor = MaterialTheme.colors.surface
    val contentColor = contentColorFor(backgroundColor = backgroundColor)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .background(
                    backgroundColor,
                    RoundedCornerShape(percent = 50)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sharingTime = contract.endTimeUTC
                ?.toLocalDateTime()
                ?.fromUTCtoMapSharingTime(
                    LocalContext.current, splitTimeToOneLine = false
                )
                ?.let {
                    stringResource(id = R.string.group_details_sharing_status_time, it)
                }
                ?: stringResource(id = R.string.group_details_sharing_status_infinite)

            Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = sharingTime.annotatedBold(),
                style = MaterialTheme.typography.caption.copy(color = contentColor)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (contract.shareBattery) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareSpeed) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareElevation) {
                Icon(
                    imageVector = Icons.Rounded.StackedLineChart,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.approximateLocationInMeters != null) {
                Icon(
                    imageVector = Icons.Rounded.ShareLocation,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 5.dp, end = 2.dp)
                        .padding(vertical = 5.dp)
                        .size(18.dp)
                )
                Text(
                    text = DistanceFormatter.getDistanceSmall(
                        LocalContext.current,
                        contract.approximateLocationInMeters!!.toFloat()
                    ).let { "($it)" },
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
                Spacer(modifier = Modifier.size(5.dp))
            }
        }

    }
}
