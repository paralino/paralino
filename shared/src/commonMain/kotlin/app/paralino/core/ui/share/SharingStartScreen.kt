package app.paralino.core.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.core.MR
import app.paralino.core.models.AppBuildConfig
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.share.SharingStartScreenEvent
import app.paralino.core.presentation.share.SharingStartScreenUiEvent
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.provider.localizedString
import app.paralino.core.ui.CoreUiState
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.BottomSheetPickerType
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.MessageDispatcherEffect
import app.paralino.core.ui.components.NoticeInfoChip
import app.paralino.core.ui.components.OnboardingSkipButton
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.core.ui.components.dialogs.AlertDialogAction
import app.paralino.core.ui.components.dialogs.LoadingDialog
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.groups.GroupListItem
import app.paralino.core.ui.onboarding.BaseOnboardingScreen
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
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
    groupId: String?,
    vm: SharingStartViewModel,
    onSharingStarted: () -> Unit,
    onSkipOnboarding: () -> Unit,
    onPickGroup: (BottomSheetPickerType.GroupPickerBottomSheet) -> Unit,
    openSubscriptions: () -> Unit,
    askNotificationPermission: () -> Unit,
    askLocationService: () -> Unit,
    askLocationPermission: () -> Unit,
    askBackgroundLocationPermission: () -> Unit,
) {

    MessageDispatcherEffect(vm, appState)
    val state by vm.uiState.collectAsState()

    var scrollToPosition by remember { mutableStateOf(0F) }
    val scrollState = rememberScrollState()

    val approxLocationSwitchState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = LargeSeekbarSwitchState.Seekbar(
                seekbarProgress = state.approximateLocationIndex + 1,
                seekbarRange = IntRange(1, state.approximateLocationPresetsMeters.size)
            ),
            switchState = LargeSeekbarSwitchState.Switch(state.shareApproximateLocation)
        )
    )

    val seekbarState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = LargeSeekbarSwitchState.Seekbar(
                seekbarProgress = state.sharingTimeIndex + 1,
                seekbarRange = IntRange(1, state.sharingPresetsMillis.size)
            ),
            switchState = null
        )
    )

    val shareBatteryState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareBattery)
        )
    )

    val shareSpeedState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareSpeed)
        )
    )

    val shareElevationState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
            seekbarState = null,
            switchState = LargeSeekbarSwitchState.Switch(state.shareElevation)
        )
    )

    val shareNetworkState by rememberSeekbarState(
        key = state,
        LargeSeekbarSwitchState.SeekbarSwitchState(
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
                        ConditionErrorType.Network -> vm.showSnackbar(
                            text = MR.strings.error_no_connection_snackbar.localizedString()
                        )

                        else -> {}
                    }
                }

                null -> {
                    /* do nothing */
                }

                is SharingStartScreenEvent.DevicesLimitReached -> {
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
                icon = if (state.canShare) {
                    CoreAppIcons.ShareLocation.asPainterResource()
                } else {
                    CoreAppIcons.NotListedLocation.asPainterResource()
                },
                onClick = { vm.startSharing() }
            ) {
                Text(text = stringResource(MR.strings.groups_sharing_button_start))
            }
        },
        skipButton = if (vm.isOnboarding) {
            @Composable {
                OnboardingSkipButton(onSkip = onSkipOnboarding)
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
                    bottom = it.calculateBottomPadding() + 40.dp,
                ),
        ) {

            AnimatedVisibility(visible = state.errorsAndWarnings.isNotEmpty()) {
                Column(modifier = Modifier.animateContentSize()) {
                    Spacer(modifier = Modifier
                        .size(20.dp)
                        .onGloballyPositioned { scrollToPosition = it.positionInParent().y })
                    state.errorsAndWarnings.forEach {
                        ConditionErrorItem(data = it) {
                            when (it) {
                                ConditionErrorType.GPS -> askLocationService()
                                is ConditionErrorType.PermissionLocation -> askLocationPermission()
                                ConditionErrorType.PermissionLocationBackground -> askBackgroundLocationPermission()
                                ConditionErrorType.PermissionNotifications -> askNotificationPermission()
                                ConditionErrorType.Network -> vm.showSnackbar(
                                    text = MR.strings.error_no_connection_snackbar.localizedString()
                                )

                                else -> {
                                    /* TODO review or do nothing */
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.groupState.data != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(name = stringResource(MR.strings.groups_sharing_section_title_group))
                    GroupListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (groupId.isNullOrBlank()) {
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

            Spacer(modifier = Modifier.size(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(MR.strings.groups_sharing_devices),
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Light),
                    modifier = Modifier.alpha(ContentAlpha.medium)
                )
                if (AppBuildConfig.instance.hasPreviewFeatures) {
                    Spacer(modifier = Modifier.size(8.dp))
                    PlanLimitCount(count = state.devicesCount, limit = state.devicesLimit)
                }
            }

            MainInputField(
                text = state.deviceName,
                hint = stringResource(MR.strings.groups_sharing_device_name_placeholder),
                hasError = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words,
                focusManager = focusManager,
                onInputChange = { vm.onDeviceNameChange(it) }
            )

            SettingsSectionHeader(name = stringResource(MR.strings.title_settings))

            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                icon = CoreAppIcons.AccessTime.asPainterResource(),
                title = stringResource(MR.strings.groups_sharing_section_title_duration),
                description = getTime(state.sharingTimeMillis),
                focusDescription = true,
                state = seekbarState,
                onProgressChange = {
                    vm.sharingDurationChanged(it - 1)
                }
            )

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
                    icon = CoreAppIcons.StackedLineChart.asPainterResource(),
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
//                LargeSeekbarSwitch(
//                    modifier = Modifier.fillMaxWidth(),
//                    icon = AppIcons.ShareLocation,
//                    title = stringResource(id = MR.strings.groups_sharing_setting_approximate_location),
//                    description = if (state.shareApproximateLocation)
//                        DistanceFormatter.getDistanceSmall(
//                            LocalContext.current,
//                            state.approximateLocationMeters.toFloat()
//                        )
//                    else null,
//                    state = approxLocationSwitchState,
//                    onProgressChange = {
//                        vm.approximateLocationChanged(it - 1)
//                    },
//                    onStateChange = {
//                        vm.turnOnApproximateLocationSharing(it)
//                    }
//                )
            }

            Spacer(modifier = Modifier.size(10.dp))

            val notifyOthersState by rememberSeekbarState(
                key = state,
                LargeSeekbarSwitchState.SeekbarSwitchState(
                    seekbarState = null,
                    switchState = LargeSeekbarSwitchState.Switch(state.notifyOthers)
                )
            )
            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
                icon = CoreAppIcons.NotificationsActive.asPainterResource(),
                title = stringResource(MR.strings.groups_sharing_setting_notifyOthers_title),
                description = stringResource(MR.strings.groups_sharing_setting_notifyOthers_description),
                state = notifyOthersState,
                onStateChange = {
                    vm.toggleNotifyOthers(it)
                }
            )

            NoticeInfoChip(
                modifier = Modifier
                    .padding(top = 40.dp),
                text = stringResource(MR.strings.groups_sharing_encryption_info)
            )
        }
    }

    if (state.loadingAction) {
        LoadingDialog(text = stringResource(MR.strings.login_step_generic_loading), progress = null)
    }
}

@Composable
fun getTime(milis: Long): String {
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
