package app.paralino.android.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.core.ui.components.BottomSheetsScreen
import app.paralino.android.ui.components.Show
import app.paralino.core.ui.groups.GroupListItem
import app.paralino.core.ui.components.SettingsSectionHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.AppIcons
import app.paralino.android.utils.extensions.quantityStringResource
import app.paralino.core.utils.extensions.safeCast
import app.paralino.core.models.Payload
import app.paralino.core.models.data
import app.paralino.core.presentation.groups.GroupItemState
import app.paralino.core.presentation.share.SharingStartScreenEvent
import app.paralino.core.presentation.share.SharingStartScreenState
import app.paralino.core.presentation.share.SharingStartViewModel
import app.paralino.core.ui.components.AppButtonState
import app.paralino.core.ui.components.ConditionErrorItem
import app.paralino.core.ui.components.CoreAppIcons
import app.paralino.core.ui.components.CoreAppIcons.asPainterResource
import app.paralino.core.ui.components.FilledButton
import app.paralino.core.ui.components.LargeSeekbarSwitch
import app.paralino.core.ui.components.LargeSeekbarSwitchState
import app.paralino.core.ui.components.MainAlertDialog
import app.paralino.core.ui.components.MainInputField
import app.paralino.core.ui.components.PlanLimitCount
import app.paralino.core.ui.components.nameRes
import app.paralino.core.ui.components.rememberSeekbarState
import app.paralino.core.ui.theme.ButtonStyle
import app.paralino.core.utils.ConditionErrorType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration
import kotlin.math.roundToInt

@Composable
fun SharingStartScreen(
    appState: MainAppState,
    groupId: String?,
    vm: SharingStartViewModel = getViewModel { parametersOf(groupId) },
) {

    val state by vm.uiState.collectAsState(
        initial = SharingStartScreenState(
            groupState = if (groupId.isNullOrBlank()) Payload.Success(null) else Payload.Loading
        )
    )

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
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

    val permissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        locationStatusProvider = vm.locationStatusProvider
    )

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is SharingStartScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                SharingStartScreenEvent.Started -> {
                    delay(400)
                    appState.navController.popBackStack()
                }
                SharingStartScreenEvent.LocationStatusInsufficient -> {
                    showDialog =
                        MainAlertDialog.SharingErrors(state.errors.map { context.getString(it.nameRes.resourceId) })
                }
                SharingStartScreenEvent.MissingBackgroundPermission -> {
                    showDialog = MainAlertDialog.SharingBackgroundLocationMissing
                }
                SharingStartScreenEvent.SelectGroupInfo -> {
                    appState.showSnackbar(context.getString(R.string.invite_connect_alert_pickGroup))
                }
                SharingStartScreenEvent.CanNotRegisterDevice -> {
                    appState.showSnackbar(context.getString(R.string.error_generic_snacbar))
                }
                null -> {
                    /* do nothing */
                }
                is SharingStartScreenEvent.UnverifiedGroup -> {
                    showDialog = MainAlertDialog.UnverifiedGroupInfo
                }
                is SharingStartScreenEvent.DevicesLimitReached -> {
                    showDialog = MainAlertDialog.PlanLimit(
                        context.getString(R.string.plan_limit_devices_alert_info),
                        limit = event.limit
                    )
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.SharingBackgroundLocationMissing -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    permissionHandler.askBackgroundLocationPermission()
                } else {
                    vm.proceedWithSharing()
                }
            }
        }
        is MainAlertDialog.UnverifiedGroupInfo -> {
            dialog.Show {
                showDialog = null
            }
        }
        is MainAlertDialog.SharingErrors -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
//                    vm.proceedWithSharing()
                    appState.uiScope.launch {
                        scrollState.animateScrollTo(
                            scrollToPosition.roundToInt(),
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                    when (state.errors.firstOrNull()) {
                        ConditionErrorType.GPS -> permissionHandler.askLocationService()
                        ConditionErrorType.PERMISSION_LOCATION -> permissionHandler.askLocationPermission()
                        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> permissionHandler.askBackgroundLocationPermission()
                        ConditionErrorType.NETWORK -> appState.showSnackbar(
                            text = context.getString(R.string.error_no_connection_snackbar)
                        )
                        else -> {}
                    }
                }
            }
        }
        is MainAlertDialog.PlanLimit -> {
            dialog.Show {
                showDialog = null
            }
        }
        else -> {}
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.background,
                elevation = 1.dp,
                title = { Text(text = stringResource(id = R.string.groups_sharing_title)) },
                navigationIcon = {
                    IconButton(onClick = { appState.navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background)
                    .padding(bottom = 20.dp, top = 8.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                FilledButton(
                    modifier = Modifier,
                    style = ButtonStyle.BIG(iconSize = 24.dp),
                    state = AppButtonState.CONTENT,
                    icon = if (state.canShare) AppIcons.ShareLocation
                    else AppIcons.NotListedLocation,
                    onClick = { vm.startSharing() }
                ) {
                    Text(text = stringResource(id = R.string.groups_sharing_button_start))
                }
            }
        },
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
                    bottom = it.calculateBottomPadding() + 100.dp,
                ),
        ) {

            AnimatedVisibility(visible = state.errors.isNotEmpty()) {
                Column(modifier = Modifier.animateContentSize()) {
                    Spacer(modifier = Modifier
                        .size(20.dp)
                        .onGloballyPositioned { scrollToPosition = it.positionInParent().y })
                    state.errors.forEach {
                        ConditionErrorItem(data = it) {
                            when (it) {
                                ConditionErrorType.GPS -> permissionHandler.askLocationService()
                                ConditionErrorType.PERMISSION_LOCATION -> permissionHandler.askLocationPermission()
                                ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> permissionHandler.askBackgroundLocationPermission()
                                ConditionErrorType.NETWORK -> appState.showSnackbar(
                                    text = context.getString(
                                        R.string.error_no_connection_snackbar
                                    )
                                )
                                else -> { /* TODO review or do nothing */
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.groupState.data != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(name = stringResource(id = R.string.groups_sharing_section_title_group))
                    GroupListItem(
                        modifier = Modifier.fillMaxWidth(),
                        data = state.groupState.data!!,
                        onEdit = {},
                        onClick = {
                            if (groupId.isNullOrBlank()) {
                                appState.uiScope.launch {
                                    appState.showBottomSheetForResult(BottomSheetsScreen.GroupPicker(vm.getAllGroupItems()))
                                        .safeCast<GroupItemState>()?.let {
                                            vm.setSelectedGroup(it.id)
                                        }
                                }
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = state.groupState !is Payload.Loading && state.groupState.data == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.size(20.dp))
                    FilledButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = ButtonStyle.SMALL,
                        icon = AppIcons.Groups,
                        onClick = {
                            appState.uiScope.launch {
                                appState.showBottomSheetForResult(BottomSheetsScreen.GroupPicker(vm.getAllGroupItems()))
                                    .safeCast<GroupItemState>()?.let {
                                        vm.setSelectedGroup(it.id)
                                    }
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.invite_connect_button_pickGroup))
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.size(20.dp))

            MainInputField(
                text = state.deviceName,
                hint = stringResource(id = R.string.groups_sharing_device_name_placeholder),
                hasError = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words,
                focusManager = focusManager,
                onInputChange = { vm.onDeviceNameChange(it) }
            )

            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(percent = 20)
                    )
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.groups_sharing_devices),
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.alpha(ContentAlpha.medium)
                )
                Spacer(modifier = Modifier.size(8.dp))
                PlanLimitCount(count = state.devicesCount, limit = state.devicesLimit)
            }

            SettingsSectionHeader(name = stringResource(id = R.string.title_settings))

            LargeSeekbarSwitch(
                modifier = Modifier.fillMaxWidth(),
                icon = AppIcons.AccessTime,
                title = stringResource(id = R.string.groups_sharing_section_title_duration),
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
                    icon = AppIcons.Speed,
                    title = stringResource(id = R.string.groups_sharing_setting_speed_share),
                    description = null,
                    state = shareSpeedState,
                    onStateChange = {
                        vm.turnOnSpeedSharing(it)
                    }
                )
                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    icon = AppIcons.StackedLineChart,
                    title = stringResource(id = R.string.groups_sharing_setting_elevation_share),
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
                    icon = AppIcons.BatteryChargingFull,
                    title = stringResource(id = R.string.groups_sharing_setting_battery_share),
                    description = null,
                    state = shareBatteryState,
                    onStateChange = {
                        vm.turnOnBatterySharing(it)
                    }
                )
                LargeSeekbarSwitch(
                    modifier = Modifier.fillMaxWidth(),
                    icon = CoreAppIcons.SignalCell3.asPainterResource(),
                    title = stringResource(id = R.string.groups_sharing_setting_network_share),
                    description = null,
                    state = shareNetworkState,
                    onStateChange = {
                        vm.turnOnNetworkSharing(it)
                    }
                )
//                LargeSeekbarSwitch(
//                    modifier = Modifier.fillMaxWidth(),
//                    icon = AppIcons.ShareLocation,
//                    title = stringResource(id = R.string.groups_sharing_setting_approximate_location),
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
fun getTime(milis: Long): String {
    val duration = Duration.ofMillis(milis)

    @Composable
    fun getPluralTime(id: Int, value: Long): String {
        return quantityStringResource(id, value.toInt(), value.toInt())
    }

    return if (duration.toDays() >= 365) {
        getPluralTime(R.plurals.groups_sharing_duration_type_years, duration.toDays() / 365)
    } else if (duration.toDays() >= 30) {
        getPluralTime(R.plurals.groups_sharing_duration_type_months, duration.toDays() / 30)
    } else if (duration.toDays() >= 1) {
        getPluralTime(R.plurals.groups_sharing_duration_type_days, duration.toDays())
    } else if (duration.toHours() >= 1) {
        getPluralTime(R.plurals.groups_sharing_duration_type_hours, duration.toHours())
    } else if (duration.toMinutes() > 1) {
        getPluralTime(R.plurals.groups_sharing_duration_type_minutes, duration.toMinutes())
    } else {
        stringResource(R.string.groups_sharing_duration_type_infinite)
    }
}
