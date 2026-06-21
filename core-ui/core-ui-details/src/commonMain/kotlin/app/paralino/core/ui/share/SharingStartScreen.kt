package app.paralino.core.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.paralino.core.MR
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.common.AlertDialogAction
import app.paralino.core.presentation.common.BottomSheetPickerType
import app.paralino.core.presentation.share.SharingStartScreenEvent
import app.paralino.core.presentation.share.SharingStartScreenState
import app.paralino.core.presentation.share.SharingStartScreenUiEvent
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.resources.CoreAppIcons
import app.paralino.core.resources.CoreAppIcons.asPainterResource
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BaseOnboardingScreen
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.DataInfoChip
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.PlanLimitSmallUpgradeButton
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.components.GroupListItem
import app.paralino.core.ui.theme.AppColors
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import app.paralino.core.utils.extensions.annotatedBoldCompose
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SharingStartScreenCore(
    appState: CoreUiState,
    vm: SharingStartViewModel,
    onSharingStarted: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
    openSubscriptions: () -> Unit,
    askNotificationPermission: () -> Unit,
    askLocationService: () -> Unit,
    askLocationPermission: () -> Unit,
    askBackgroundLocationPermission: () -> Unit,
    askBackgroundAccessPermission: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    var scrollToPosition by remember { mutableStateOf(0F) }
    val scrollState = rememberScrollState()

    val seekbarState by rememberSeekbarState(
        key = state,
        state = LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = LargeSeekbarSwitchState.Seekbar(
                seekbarProgress = state.sharingTimeIndex + 1,
                seekbarRange = IntRange(1, state.sharingPresetsMillis.size)
            ),
            switchState = null
        )
    )

    val shareBatteryState by rememberSeekbarState(
        key = state,
        state = LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareBattery)
        )
    )

    val shareSpeedState by rememberSeekbarState(
        key = state,
        state = LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareSpeed)
        )
    )

    val shareElevationState by rememberSeekbarState(
        key = state,
        state = LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareElevation)
        )
    )

    val shareNetworkState by rememberSeekbarState(
        key = state,
        state = LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareNetwork)
        )
    )

    LaunchedEffect(key1 = Unit) {
        vm.events.collectLatest { event ->
            when (event) {
                SharingStartScreenEvent.Started -> {
                    delay(400)
                    onSharingStarted()
                }

                SharingStartScreenEvent.Updated -> {
                    onSharingStarted()
                }

                SharingStartScreenEvent.AdjustScrollPosition -> {
                    scrollState.animateScrollTo(
                        scrollToPosition.roundToInt(),
                        animationSpec = tween(durationMillis = 500)
                    )
                }

                is SharingStartScreenEvent.AskForPermission -> {
                    when (event.error) {
                        ConditionErrorType.GPS -> askLocationService()
                        is ConditionErrorType.PermissionLocation -> askLocationPermission()
                        ConditionErrorType.PermissionLocationBackground -> askBackgroundLocationPermission()
                        ConditionErrorType.PermissionNotifications -> askNotificationPermission()
                        ConditionErrorType.UnrestrictedBackgroundAccess -> askBackgroundAccessPermission()
                        ConditionErrorType.Network -> vm.showSnackbar(
                            text = MR.strings.error_no_connection_snackbar.localizedString()
                        )

                        else -> {}
                    }
                }

                null -> {
                    /* do nothing */
                }

                is SharingStartScreenEvent.OpenPaywall -> {
                    openSubscriptions()
                }
            }
        }
    }

    val dialog = state.alertDialog
    dialog?.Show {
        vm.onEvent(SharingStartScreenUiEvent.DialogAction(AlertDialogAction(dialog, it)))
    }

    BaseOnboardingScreen(
        modifier = Modifier.fillMaxSize(),
        nextButton = @Composable {
            FilledButton(
                modifier = Modifier,
                style = ButtonStyle.BIG,
                state = AppButtonState.loadingState(state.loadingAction),
                enabled = state.isStartButtonEnabled,
                icon = if (!state.isNewContract) {
                    null
                } else if (state.canShare) {
                    CoreAppIcons.ShareLocation.asPainterResource()
                } else {
                    CoreAppIcons.NotListedLocation.asPainterResource()
                },
                onClick = { vm.onEvent(SharingStartScreenUiEvent.StartSharing) }
            ) {
                Text(
                    text = if (!state.isNewContract) {
                        stringResource(MR.strings.button_save)
                    } else if (state.isPersonalSpace) {
                        stringResource(MR.strings.location_sharing_button_start_personal)
                    } else {
                        stringResource(MR.strings.location_sharing_button_start_groups)
                    }
                )
            }
        },
        skipButton = if (vm.isOnboarding) {
            @Composable {
                Column {
                    if (vm.isPersonalSpace && state.shouldUpgradePlan) {
                        PlanLimitSmallUpgradeButton(
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = stringResource(MR.strings.groups_sharing_plan_limit_chip_owner),
                            onClick = { openSubscriptions() }
                        )
                    }

                    OnboardingSkipButton(onSkip = onSkipOnboarding)
                }
            }
        } else if (!state.isNewContract) {
            @Composable {
                OnboardingSkipButton(
                    skipButtonText = if (state.isPersonalSpace) {
                        stringResource(MR.strings.location_sharing_button_stop_personal)
                    } else {
                        stringResource(MR.strings.location_sharing_button_stop_groups)
                    },
                    onSkip = { vm.onEvent(SharingStartScreenUiEvent.StopSharing) }
                )
            }
        } else if (state.shouldUpgradePlan) {
            @Composable {
                PlanLimitSmallUpgradeButton(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(MR.strings.groups_sharing_plan_limit_chip_owner),
                    onClick = { openSubscriptions() }
                )
            }
        } else if (state.needsPlanUpgrade) {
            @Composable {
                PlanLimitSmallUpgradeButton(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(MR.strings.groups_sharing_plan_limit_chip_member),
                    onClick = { vm.onEvent(SharingStartScreenUiEvent.StartSharing) }
                )
            }
        } else {
            null
        }
    ) {
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .animateContentSize()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = it.calculateBottomPadding() + 40.dp
                ),
        ) {
            AnimatedVisibility(visible = state.areLimitsExceeded) {
                NoticeInfoChip(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                    text = if (state.isPersonalSpace) {
                        stringResource(MR.strings.plan_limit_overLimit_tracking_banner_title)
                    } else {
                        stringResource(MR.strings.plan_limit_overLimit_sharing_banner_title)
                    },
                    icon = CoreAppIcons.Warning.asPainterResource(),
                    backgroundColor = MaterialTheme.colors.error,
                    textColor = MaterialTheme.colors.onError,
                    iconTint = MaterialTheme.colors.onError
                )
            }

            AnimatedVisibility(visible = state.errorsAndWarnings.isNotEmpty()) {
                Column(modifier = Modifier.animateContentSize()) {
                    Spacer(
                        modifier = Modifier
                            .size(20.dp)
                            .onGloballyPositioned { scrollToPosition = it.positionInParent().y })
                    state.errorsAndWarnings.forEach {
                        ConditionErrorItem(data = it) {
                            when (it) {
                                ConditionErrorType.GPS -> askLocationService()
                                is ConditionErrorType.PermissionLocation -> askLocationPermission()
                                ConditionErrorType.PermissionLocationBackground -> askBackgroundLocationPermission()
                                ConditionErrorType.PermissionNotifications -> askNotificationPermission()
                                ConditionErrorType.UnrestrictedBackgroundAccess -> askBackgroundAccessPermission()
                                ConditionErrorType.Network -> vm.showSnackbar(
                                    text = MR.strings.error_no_connection_snackbar.localizedString()
                                )

                                else -> {}
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.groupState.data != null && !state.canChangeGroup) {
                DataInfoChip(
                    modifier = Modifier.padding(top = 20.dp),
                    image = state.groupState.data?.image,
                    text = state.groupState.data?.name ?: "",
                    backgroundColor = if (state.isPersonalSpace) {
                        AppColors.accent.copy(0.1f)
                    } else {
                        MaterialTheme.colors.surface
                    }
                )
            }

            if (vm.isOnboarding) {
                NoticeInfoChip(
                    modifier = Modifier.padding(top = 10.dp),
                    text = if (state.isPersonalSpace) {
                        stringResource(MR.strings.personalSpace_tracking_encryption_info)
                    } else {
                        stringResource(MR.strings.groups_sharing_encryption_info)
                    }
                )
            }

            AnimatedVisibility(visible = state.groupState.data != null && state.canChangeGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(name = stringResource(MR.strings.groups_sharing_section_title_group))
                    GroupListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (state.canChangeGroup) {
                                    appState.uiScope.launch {
                                        val groupPickerState = vm.getGroupPickerState()
                                        onPickGroup(groupPickerState)
                                    }
                                }
                            },
                        data = state.groupState.data!!,
                        onEdit = {}
                    )
                }
            }

            AnimatedVisibility(visible = state.groupState !is Payload.Loading && state.groupState.data == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.size(20.dp))
                    FilledButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = ButtonStyle.SMALL,
                        icon = CoreAppIcons.Groups.asPainterResource(),
                        onClick = {
                            appState.uiScope.launch {
                                val groupPickerState = vm.getGroupPickerState()
                                onPickGroup(groupPickerState)
                            }
                        }
                    ) {
                        Text(text = stringResource(MR.strings.invite_connect_button_pickGroup))
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(visible = state.canShowContent) {
                Column {
                    AnimatedVisibility(visible = state.deviceName != null) {
                        Column {
                            Spacer(modifier = Modifier.size(10.dp))
                            SettingsSectionHeader(
                                modifier = Modifier.fillMaxWidth(),
                                name = stringResource(MR.strings.groups_sharing_devices),
                                count = state.devicesCount,
                                limit = state.devicesLimit
                            )

                            MainInputField(
                                modifier = Modifier.heightIn(min = 70.dp),
                                text = state.deviceName ?: "",
                                hint = stringResource(MR.strings.groups_sharing_device_name_placeholder),
                                maxTextLength = SharingStartScreenState.maxDeviceNameLength,
                                hasError = false,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Words,
                                focusManager = focusManager,
                                onInputChange = { vm.onDeviceNameChange(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(10.dp))
                    SettingsSectionHeader(name = stringResource(MR.strings.title_settings))

                    AnimatedVisibility(visible = state.formattedEndTime != null) {
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colors.surface,
                                    RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sharingTime = state.formattedEndTime ?: ""

                            Icon(
                                painter = CoreAppIcons.Timer.asPainterResource(),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colors.onSurface
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = sharingTime.annotatedBoldCompose(),
                                style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface)
                            )
                        }
                    }

                    if (state.showDuration) {
                        LargeSeekbarSwitch(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            icon = CoreAppIcons.AccessTime.asPainterResource(),
                            title = stringResource(MR.strings.groups_sharing_section_title_duration),
                            description = getSharingTimeString(state.sharingTimeMillis),
                            focusDescription = true,
                            state = seekbarState,
                            onProgressChange = {
                                vm.sharingDurationChanged(it - 1)
                            }
                        )
                    }

                    if (state.showMetadata) {
                        Spacer(modifier = Modifier.size(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LargeSeekbarSwitch(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .fillMaxWidth(0.5f),
                                icon = CoreAppIcons.Speed.asPainterResource(),
                                title = stringResource(MR.strings.groups_sharing_setting_speed_share),
                                description = null,
                                state = shareSpeedState,
                                onStateChange = {
                                    vm.turnOnSpeedSharing(it)
                                }
                            )
                            LargeSeekbarSwitch(
                                modifier = Modifier.fillMaxWidth(),
                                icon = CoreAppIcons.Elevation.asPainterResource(),
                                title = stringResource(MR.strings.groups_sharing_setting_elevation_share),
                                description = null,
                                state = shareElevationState,
                                onStateChange = {
                                    vm.turnOnElevationSharing(it)
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
                                icon = CoreAppIcons.BatteryChargingFull.asPainterResource(),
                                title = stringResource(MR.strings.groups_sharing_setting_battery_share),
                                description = null,
                                state = shareBatteryState,
                                onStateChange = {
                                    vm.turnOnBatterySharing(it)
                                }
                            )
                            LargeSeekbarSwitch(
                                modifier = Modifier.fillMaxWidth(),
                                icon = CoreAppIcons.SignalCell3.asPainterResource(),
                                title = stringResource(MR.strings.groups_sharing_setting_network_share),
                                description = null,
                                state = shareNetworkState,
                                onStateChange = {
                                    vm.turnOnNetworkSharing(it)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(10.dp))
                    LargeSeekbarSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = CoreAppIcons.TimelinePathWithPins.asPainterResource(),
                        title = stringResource(MR.strings.groups_sharing_setting_timeline_title),
                        description = stringResource(MR.strings.groups_sharing_setting_timeline_description),
                        state = rememberSeekbarState(
                            key = state,
                            state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                seekbarState = null,
                                switchState = LargeSeekbarSwitchState.Switch(state.saveToHistory)
                            )
                        ).value,
                        onStateChange = {
                            vm.turnOnLocationHistory(it)
                        }
                    )

                    if (state.showApproximateLocation) {
                        Spacer(modifier = Modifier.size(10.dp))
                        LargeSeekbarSwitch(
                            modifier = Modifier.fillMaxWidth(),
                            icon = CoreAppIcons.NotListedLocation.asPainterResource(),
                            title = stringResource(MR.strings.groups_sharing_setting_approximateLocation_title),
                            description = if (state.shareApproximateLocation) {
                                stringResource(MR.strings.groups_sharing_setting_approximateLocation_description_on)
                            } else {
                                stringResource(MR.strings.groups_sharing_setting_approximateLocation_description_off)
                            },
                            bigTag = if (state.shareApproximateLocation) {
                                state.formattedApproximateLocation
                            } else {
                                null
                            },
                            isMinimumVerticalSpacing = !state.shareApproximateLocation,
                            state = rememberSeekbarState(
                                key = state,
                                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                    seekbarState = LargeSeekbarSwitchState.Seekbar(
                                        seekbarProgress = state.approximateLocationIndex + 1,
                                        seekbarRange = IntRange(
                                            start = 1,
                                            endInclusive = state.approximateLocationPresets.size
                                        )
                                    ),
                                    switchState = LargeSeekbarSwitchState.Switch(state.shareApproximateLocation)
                                )
                            ).value,
                            focusDescription = false,
                            onProgressChange = {
                                vm.approximateLocationChanged(it - 1)
                            },
                            onStateChange = {
                                vm.turnOnApproximateLocationSharing(it)
                            }
                        )
                    }

                    if (state.showDeviceStatus) {
                        Spacer(modifier = Modifier.size(10.dp))
                        LargeSeekbarSwitch(
                            modifier = Modifier.fillMaxWidth(),
                            icon = CoreAppIcons.PhoneAndroid.asPainterResource(),
                            title = stringResource(MR.strings.groups_sharing_setting_deviceStatus_title),
                            description = stringResource(
                                MR.strings.groups_sharing_setting_deviceStatus_description,
                                stringResource(MR.strings.group_members_status_inactive)
                            ),
                            state = rememberSeekbarState(
                                key = state,
                                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                    seekbarState = null,
                                    switchState = LargeSeekbarSwitchState.Switch(state.shareDeviceStatus)
                                )
                            ).value,
                            onStateChange = {
                                vm.turnOnDeviceStatusSharing(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    if (state.isPremiumSectionHeaderVisible) {
                        SettingsSectionHeader(
                            modifier = Modifier.zIndex(0f),
                            backgroundColor = Color.Transparent,
                            name = stringResource(MR.strings.setting_location_premium_banner_title),
                            end = {
                                Icon(
                                    painter = CoreAppIcons.StarFamily.asPainterResource(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { openSubscriptions() }
                                        .background(
                                            color = AppColors.highlightGold,
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }

                    if (state.showRealtimeUpdatesStatus) {
                        LargeSeekbarSwitch(
                            modifier = Modifier.fillMaxWidth(),
                            icon = CoreAppIcons.ShareLocation.asPainterResource(),
                            title = stringResource(MR.strings.groups_sharing_setting_realtimeUpdates_title),
                            description = if (state.isPremiumSectionHeaderVisible || state.isRealtimeOptionSelected) {
                                stringResource(MR.strings.groups_sharing_setting_realtimeUpdates_description_on)
                            } else {
                                stringResource(MR.strings.groups_sharing_setting_realtimeUpdates_description_off)
                            },
                            state = rememberSeekbarState(
                                key = state,
                                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                    seekbarState = null,
                                    switchState = LargeSeekbarSwitchState.Switch(state.isRealtimeOptionSelected)
                                )
                            ).value,
                            onStateChange = {
                                vm.toggleRealtimeUpdates(it)
                            }
                        )
                    }

                    if (state.showNotifyOthers) {
                        Spacer(modifier = Modifier.size(10.dp))
                        LargeSeekbarSwitch(
                            modifier = Modifier.fillMaxWidth(),
                            icon = CoreAppIcons.NotificationsActive.asPainterResource(),
                            title = stringResource(MR.strings.groups_sharing_setting_notifyOthers_title),
                            description = stringResource(MR.strings.groups_sharing_setting_notifyOthers_description),
                            state = rememberSeekbarState(
                                key = state,
                                state = LargeSeekbarSwitchState.SeekbarSwitchState(
                                    seekbarState = null,
                                    switchState = LargeSeekbarSwitchState.Switch(state.notifyOthers)
                                )
                            ).value,
                            onStateChange = {
                                vm.toggleNotifyOthers(it)
                            }
                        )
                    }
                }
            }

            if (!vm.isOnboarding) {
                Divider(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    color = Color.Gray.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
                NoticeInfoChip(
                    modifier = Modifier.padding(top = 20.dp),
                    text = if (state.isPersonalSpace) {
                        stringResource(MR.strings.personalSpace_tracking_encryption_info)
                    } else {
                        stringResource(MR.strings.groups_sharing_encryption_info)
                    }
                )
            }
        }
    }

    if (state.loadingAction) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}

@Composable
fun getSharingTimeString(
    milis: Long
): String {
    val duration = milis.milliseconds

    @Composable
    fun getPluralTime(id: PluralsResource, value: Long): String {
        return stringResource(id, value.toInt(), value.toInt())
    }

    return if (duration.inWholeDays >= 365) {
        getPluralTime(MR.plurals.groups_sharing_duration_type_years, duration.inWholeDays / 365)
    } else if (duration.inWholeDays >= 30) {
        getPluralTime(MR.plurals.groups_sharing_duration_type_months, duration.inWholeDays / 30)
    } else if (duration.inWholeDays >= 1) {
        getPluralTime(MR.plurals.groups_sharing_duration_type_days, duration.inWholeDays)
    } else if (duration.inWholeHours >= 1) {
        getPluralTime(MR.plurals.groups_sharing_duration_type_hours, duration.inWholeHours)
    } else if (duration.inWholeMinutes > 1) {
        getPluralTime(MR.plurals.groups_sharing_duration_type_minutes, duration.inWholeMinutes)
    } else {
        stringResource(MR.strings.groups_sharing_duration_type_infinite)
    }
}
