package app.paralino.core.ui.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.user.BlocklistType
import app.paralino.core.presentation.groups.GroupDetailScreenEvent
import app.paralino.core.presentation.groups.GroupDetailViewModel
import app.paralino.core.presentation.groups.LocationSharingState
import app.paralino.core.presentation.groups.MemberAction
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBold
import app.paralino.core.utils.extensions.errorString
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreenCore(
    appState: CoreUiState,
    vm: GroupDetailViewModel,
    groupId: String?,
    onShowDialog: (MainAlertDialog?) -> Unit,
    openMemberAdd: () -> Unit,
    openGroupEdit: () -> Unit,
    openMap: () -> Unit,
    openPlaces: () -> Unit,
    openGroupRawData: () -> Unit,
    openMemberRawData: (String) -> Unit,
    openSharing: () -> Unit,
    onNavigateBack: () -> Unit,
    askNotificationPermission: () -> Unit,
    askLocationService: () -> Unit,
    askLocationPermission: () -> Unit,
    askBackgroundLocationPermission: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)

    if (groupId.isNullOrBlank()) {
        DefaultEmptyState(content = stringResource(MR.strings.group_load_error))
        return
    }

    val uiState by vm.uiState.collectAsState(initial = Payload.Loading)

    var optionMenuExpanded by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                is GroupDetailScreenEvent.Error -> {
                    vm.showSnackbar(event.error.errorString())
                }

                GroupDetailScreenEvent.GroupNotFound -> {
                    vm.showSnackbar(MR.strings.group_load_error.localizedString())
                }

                GroupDetailScreenEvent.GroupDeleted -> {
                    onNavigateBack()
                }

                GroupDetailScreenEvent.GroupMemberAdd -> {
                    openMemberAdd()
                }

                is GroupDetailScreenEvent.GroupMembersLimitReached -> {
                    onShowDialog(
                        MainAlertDialog.PlanLimit(
                            MR.strings.plan_limit_members_alert_info.localizedString(),
                            limit = event.limit
                        )
                    )
                }

                GroupDetailScreenEvent.UserBlockActionCompleted -> {}
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
            DefaultEmptyState(content = stringResource(MR.strings.group_load_error))
        }

        is Payload.Success -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        backgroundColor = MaterialTheme.colors.background,
                        elevation = 1.dp,
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    painter = CoreAppIcons.Close.asPainterResource(),
                                    contentDescription = null
                                )
                            }
                        },
                        actions = {

                            if (state.data.canEdit) {
                                IconButton(onClick = { vm.tryAddMember() }) {
                                    Icon(
                                        painter = CoreAppIcons.PersonAdd.asPainterResource(),
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = { openGroupEdit() }) {
                                    Icon(
                                        painter = CoreAppIcons.Edit.asPainterResource(),
                                        contentDescription = null
                                    )
                                }
                            }

                            IconButton(onClick = {
                                optionMenuExpanded = true
                            }) {
                                Icon(
                                    painter = CoreAppIcons.MoreVert.asPainterResource(),
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                expanded = optionMenuExpanded,
                                onDismissRequest = { optionMenuExpanded = false }
                            ) {
                                if (state.data.canEdit) {
                                    DropdownMenuItem(
                                        onClick = {
                                            optionMenuExpanded = false
                                            onShowDialog(MainAlertDialog.DeleteGroup)
                                        }
                                    ) {
                                        Icon(
                                            painter = CoreAppIcons.Delete.asPainterResource(),
                                            tint = MaterialTheme.colors.onError,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = stringResource(MR.strings.group_details_menu_delete)
                                        )
                                    }
                                } else {
                                    DropdownMenuItem(
                                        onClick = {
                                            optionMenuExpanded = false
                                            onShowDialog(MainAlertDialog.BlockGroup)
                                        }
                                    ) {
                                        Icon(
                                            painter = CoreAppIcons.Block.asPainterResource(),
                                            tint = MaterialTheme.colors.onError,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = stringResource(MR.strings.group_details_menu_block)
                                        )
                                    }
                                    DropdownMenuItem(
                                        onClick = {
                                            optionMenuExpanded = false
                                            onShowDialog(MainAlertDialog.LeaveGroup)
                                        }
                                    ) {
                                        Icon(
                                            painter = CoreAppIcons.Logout.asPainterResource(),
                                            tint = MaterialTheme.colors.onError,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = stringResource(MR.strings.group_details_menu_leave)
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = {
                            if (state.data.sharingStatus != null) {
                                Text(text = stringResource(MR.strings.location_banner_info_stop))
                            } else {
                                Text(text = stringResource(MR.strings.groups_sharing_button_start))
                            }
                        },
                        icon = {
                            if (state.data.sharingStatus != null) {
                                Icon(
                                    painter = CoreAppIcons.StopCircle.asPainterResource(),
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    painter = CoreAppIcons.ShareLocation.asPainterResource(),
                                    contentDescription = null
                                )
                            }
                        },
                        backgroundColor = MaterialTheme.colors.primary,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                        onClick = {
                            if (state.data.sharingStatus != null) {
                                vm.stopSharing()
                            } else {
                                openSharing()
                            }
                        }
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

                    this.items(items = state.data.errors, key = { "error_$it" }) {
                        ConditionErrorItem(data = it) {
                            when (it) {
                                ConditionErrorType.GPS -> askLocationService()
                                ConditionErrorType.PERMISSION_NOTIFICATIONS -> askNotificationPermission()
                                ConditionErrorType.PERMISSION_LOCATION -> askLocationPermission()
                                ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> askBackgroundLocationPermission()
                                ConditionErrorType.NETWORK -> vm.showSnackbar(
                                    text = MR.strings.error_no_connection_snackbar.localizedString()
                                )

                                else -> {}
                            }
                        }
                    }

                    if (state.data.errors.isNotEmpty())
                        this.item { Spacer(modifier = Modifier.size(10.dp)) }

                    if (state.data.sharingStatus != null) {
                        this.item(key = "sharing_status") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                GroupDetailSharingStatusElement(
                                    contract = state.data.sharingStatus!!
                                )
                            }
                        }
                        this.item { Spacer(modifier = Modifier.size(30.dp)) }
                    }

                    this.item(key = "main_header") {
                        GroupDetailsHeader(
                            groupName = state.data.name,
                            groupImage = state.data.image,
                            onMapClick = { openMap() },
                            onPlacesClick = { openPlaces() },
                            onSecurityClick = { openGroupRawData() },
                            onSharingClick = { openSharing() }
                        )
                    }

                    this.stickyHeader(key = "member_list_header") {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(MR.strings.group_details_section_members_title),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.alpha(ContentAlpha.medium)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            PlanLimitCount(
                                count = state.data.members.size,
                                limit = state.data.memberLimit
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    this.itemsIndexed(
                        items = state.data.members,
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
                            onActionClick = {
                                when (it) {
                                    MemberAction.DELETE_MEMBER -> {
                                        onShowDialog(
                                            MainAlertDialog.DeleteGroupMember(
                                                item.memberState.userId,
                                                item.memberState.name
                                            )
                                        )
                                    }

                                    MemberAction.BLOCK_USER -> {
                                        onShowDialog(MainAlertDialog.BlockUser(userId = item.memberState.userId))
                                    }

                                    MemberAction.UNBLOCK_USER -> {
                                        onShowDialog(
                                            MainAlertDialog.UnblockGroupOrUser(
                                                id = item.memberState.userId,
                                                type = BlocklistType.USER
                                            )
                                        )
                                    }

                                    MemberAction.DELETE_LOCATION, MemberAction.REQUEST_LOCATION -> {}
                                }
                            },
                            onDeleteDeviceClick = {
                                onShowDialog(
                                    MainAlertDialog.DeleteMemberLocation(
                                        memberId = item.memberState.memberId,
                                        locationId = it.locationId,
                                        isUserOwnDevice = item.memberState.isCurrentUser,
                                        memberName = item.memberState.name
                                    )
                                )
                            },
                            extraActions = {
                                IconButton(
                                    onClick = {
                                        openMemberRawData(item.memberState.memberId)
                                    }
                                ) {
                                    Icon(
                                        painter = CoreAppIcons.Security.asPainterResource(),
                                        tint = AppColors.accent,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(state.data.loadingAction, enter = fadeIn(), exit = fadeOut()) {
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
    }
}

@Composable
fun GroupDetailsHeader(
    modifier: Modifier = Modifier,
    groupName: String,
    groupImage: ImageModel,
    onMapClick: () -> Unit,
    onPlacesClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onSharingClick: () -> Unit,
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
                icon = CoreAppIcons.Map.asPainterResource(),
                title = stringResource(MR.strings.title_map),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onMapClick()
                }
            )
            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
                icon = CoreAppIcons.MapsHomeWork.asPainterResource(),
                title = stringResource(MR.strings.places_list_screen_title),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onPlacesClick()
                }
            )
        }

        Spacer(modifier = Modifier.size(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LargeSeekbarSwitch(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .fillMaxWidth(0.5f),
                icon = CoreAppIcons.Security.asPainterResource(),
                title = stringResource(MR.strings.group_details_security_action),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onSecurityClick()
                }
            )
            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
                icon = CoreAppIcons.ShareLocation.asPainterResource(),
                title = stringResource(MR.strings.group_details_sharing_action),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onSharingClick()
                }
            )
        }

        Spacer(modifier = Modifier.size(20.dp))
    }
}

@Composable
fun GroupDetailSharingStatusElement(
    modifier: Modifier = Modifier,
    contract: LocationSharingState,
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
            val sharingTime = contract.endTime
                ?.let { stringResource(MR.strings.group_details_sharing_status_time, it) }
                ?: stringResource(MR.strings.group_details_sharing_status_infinite)

            Icon(
                painter = CoreAppIcons.Timer.asPainterResource(),
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
                    painter = CoreAppIcons.BatteryChargingFull.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareSpeed) {
                Icon(
                    painter = CoreAppIcons.Speed.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareElevation) {
                Icon(
                    painter = CoreAppIcons.StackedLineChart.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.approximateLocation != null) {
                Icon(
                    painter = CoreAppIcons.ShareLocation.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 5.dp, end = 2.dp)
                        .padding(vertical = 5.dp)
                        .size(18.dp)
                )
                Text(
                    text = "(${contract.approximateLocation})",
                    style = MaterialTheme.typography.overline.copy(letterSpacing = 0.1.sp)
                )
                Spacer(modifier = Modifier.size(5.dp))
            }
        }

    }
}
