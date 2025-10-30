package app.paralino.core.ui.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.detail.GroupDetailScreenEvent
import app.paralino.core.presentation.groups.detail.GroupDetailScreenUiEvent
import app.paralino.core.presentation.groups.detail.GroupDetailViewModel
import app.paralino.core.presentation.groups.detail.LocationSharingState
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.DefaultEmptyState
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.ImageModelImage
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SmallActionButton
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.groups.components.GroupMember
import app.paralino.core.ui.settings.billing.PremiumPlanBannerSmall
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreenCore(
    appState: CoreUiState,
    vm: GroupDetailViewModel,
    groupId: String?,
    contentPadding: PaddingValues,
    openMemberAdd: () -> Unit,
    openMap: () -> Unit,
    openPlaces: () -> Unit,
    openGroupSettings: () -> Unit,
    openMemberRawData: (String) -> Unit,
    openSharing: () -> Unit,
    openPaywall: () -> Unit,
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

    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                GroupDetailScreenEvent.GroupDeleted -> {
                    onNavigateBack()
                }

                GroupDetailScreenEvent.GroupMemberAdd -> {
                    openMemberAdd()
                }

                is GroupDetailScreenEvent.OpenPaywall -> {
                    openPaywall()
                }

                is GroupDetailScreenEvent.OpenRawMemberData -> {
                    openMemberRawData(event.memberId)
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

    uiState.data?.alertDialog?.let { dialog ->
        dialog.Show { action ->
            vm.onEvent(
                GroupDetailScreenUiEvent.DialogAction(
                    AlertDialogAction(dialog = dialog, action = action)
                )
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            vm.onEvent(GroupDetailScreenUiEvent.OnWholeContentClick)
                        }
                    ),
                floatingActionButton = {
                    if (!state.data.isMemberFocused) {
                        if (state.data.sharingStatus != null) {
                            FilledButton(
                                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                                style = ButtonStyle.SMALL(
                                    iconSize = 24.dp
                                ),
                                icon = CoreAppIcons.StopCircle.asPainterResource(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.error,
                                    contentColor = MaterialTheme.colors.onError
                                ),
                                onClick = { vm.onEvent(GroupDetailScreenUiEvent.StopSharing) }
                            ) {
                                Text(text = stringResource(MR.strings.location_sharing_button_stop_groups))
                            }
                        } else {
                            FilledButton(
                                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                                style = ButtonStyle.SMALL(
                                    iconSize = 24.dp
                                ),
                                icon = CoreAppIcons.ShareLocation.asPainterResource(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = MaterialTheme.colors.onPrimary
                                ),
                                onClick = { openSharing() }
                            ) {
                                Text(text = stringResource(MR.strings.location_sharing_button_start_groups))
                            }
                        }
                    }
                },
                isFloatingActionButtonDocked = true
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = contentPadding.calculateTopPadding() + 20.dp,
                        bottom = contentPadding.calculateBottomPadding() + 100.dp
                    )
                ) {
                    if (state.data.arePlacesOverLimit == true) {
                        this.item(key = "over_limit_notice") {
                            NoticeInfoChip(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                text = stringResource(MR.strings.plan_limit_overLimit_places_banner_title),
                                icon = CoreAppIcons.Warning.asPainterResource(),
                                backgroundColor = MaterialTheme.colors.error,
                                textColor = MaterialTheme.colors.onError,
                                iconTint = MaterialTheme.colors.onError
                            )
                        }
                    } else if (state.data.isGroupOverLimit == true) {
                        this.item(key = "over_limit_notice") {
                            NoticeInfoChip(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                text = stringResource(MR.strings.plan_limit_overLimit_sharing_banner_title),
                                icon = CoreAppIcons.Warning.asPainterResource(),
                                backgroundColor = MaterialTheme.colors.error,
                                textColor = MaterialTheme.colors.onError,
                                iconTint = MaterialTheme.colors.onError
                            )
                        }
                    }

                    this.items(items = state.data.errors, key = { "error_$it" }) {
                        ConditionErrorItem(data = it) {
                            when (it) {
                                ConditionErrorType.GPS -> askLocationService()
                                ConditionErrorType.PermissionNotifications -> askNotificationPermission()
                                is ConditionErrorType.PermissionLocation -> askLocationPermission()
                                ConditionErrorType.PermissionLocationBackground -> askBackgroundLocationPermission()
                                ConditionErrorType.Network -> vm.showSnackbar(
                                    text = MR.strings.error_no_connection_snackbar.localizedString()
                                )

                                else -> {}
                            }
                        }
                    }

                    if (state.data.errors.isNotEmpty())
                        this.item { Spacer(modifier = Modifier.size(10.dp)) }

                    if (state.data.sharingStatus != null && !state.data.isMemberFocused) {
                        this.item(key = "sharing_status") {
                            Box(
                                modifier = Modifier.fillMaxWidth().animateItem(),
                                contentAlignment = Alignment.Center
                            ) {
                                GroupDetailSharingStatusElement(
                                    contract = state.data.sharingStatus!!,
                                    isPersonalSpace = false
                                )
                            }
                        }
                        this.item { Spacer(modifier = Modifier.size(20.dp).animateItem()) }
                    }

                    if (!state.data.isMemberFocused) {
                        this.item(key = "main_header") {
                            GroupDetailsHeader(
                                modifier = Modifier.animateItem(),
                                groupName = state.data.name,
                                groupImage = state.data.image,
                                onMapClick = { openMap() },
                                onPlacesClick = { openPlaces() },
                                onSettingsClick = openGroupSettings,
                                onSharingClick = { openSharing() }
                            )
                        }
                    }

                    if (state.data.shouldShowPlanBanner && !state.data.isMemberFocused) {
                        this.item(key = "plan_upgrade_banner") {
                            PremiumPlanBannerSmall(
                                modifier = Modifier.zIndex(1f).animateItem(),
                                description = stringResource(MR.strings.plans_banner_inline_group_detail),
                                onClick = openPaywall
                            )
                            Spacer(modifier = Modifier.size(30.dp).animateItem())
                        }
                    }

                    if (!state.data.isMemberFocused) {
                        this.stickyHeader(key = "member_list_header") {
                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .zIndex(0f)
                                    .fillParentMaxWidth()
                                    .background(MaterialTheme.colors.background)
                                    .padding(vertical = 8.dp)
                                    .padding(bottom = 4.dp),
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
                                    limit = if (state.data.canShowMembersLimit) {
                                        state.data.membersLimit
                                    } else {
                                        null
                                    }
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (state.data.canEdit) {
                                    SmallActionButton(
                                        modifier = Modifier,
                                        icon = CoreAppIcons.Add.asPainterResource(),
                                        text = {
                                            Text(
                                                modifier = Modifier.padding(end = 2.dp),
                                                text = stringResource(MR.strings.group_details_add_member_button)
                                            )
                                        },
                                        backgroundColor = MaterialTheme.colors.primary,
                                        contentColor = MaterialTheme.colors.onPrimary,
                                        onClick = { vm.onEvent(GroupDetailScreenUiEvent.OnAddNewMember) }
                                    )
                                }
                            }
                        }
                    }

                    this.itemsIndexed(
                        items = state.data.visibleMembers,
                        key = { index, item -> item.memberState.memberId }
                    ) { index, item ->
                        GroupMember(
                            modifier = Modifier
                                .animateItem()
                                .padding(top = if (index > 0) 8.dp else 0.dp),
                            state = item,
                            forceExpandAction = item.memberState.memberId == state.data.focusedMemberId,
                            collapseOnActionClick = false,
                            onEvent = { event ->
                                vm.onEvent(GroupDetailScreenUiEvent.OnMemberItemEvent(event))
                            }
                        )
                    }
                }
            }

            if (state.data.loadingAction) {
                LoadingDialog(
                    text = stringResource(MR.strings.login_step_generic_loading),
                    progress = null
                )
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
    onSettingsClick: () -> Unit,
    onSharingClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageModelImage(
                imageModel = groupImage,
                modifier = Modifier
                    .size(56.dp)
                    .border(
                        0.5.dp,
                        MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.disabled),
                        RoundedCornerShape(10.dp)
                    )
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = groupName,
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.size(30.dp))

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
                icon = CoreAppIcons.Settings.asPainterResource(),
                title = stringResource(MR.strings.title_settings),
                description = null,
                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                    switchState = LargeSeekbarSwitchState.Switch(
                        checkState = false,
                        unCheckedIconRes = CoreAppIcons.ChevronRight
                    ),
                    seekbarState = null
                ),
                onStateChange = {
                    onSettingsClick()
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
    isPersonalSpace: Boolean
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
                ?: let {
                    if (isPersonalSpace) {
                        stringResource(MR.strings.personalSpace_tracking_status_infinite)
                    } else {
                        stringResource(MR.strings.group_details_sharing_status_infinite)
                    }
                }

            Icon(
                painter = CoreAppIcons.Timer.asPainterResource(),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = sharingTime.annotatedBoldCompose(),
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
                    painter = CoreAppIcons.Elevation.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareNetwork) {
                Icon(
                    painter = CoreAppIcons.SignalCell3.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            if (contract.shareDeviceStatus) {
                Icon(
                    painter = CoreAppIcons.PhoneAndroid.asPainterResource(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }


            if (contract.saveToHistory) {
                Icon(
                    painter = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
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
