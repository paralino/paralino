package com.zbapp.locationtrack.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.presentation.share.SharingStartScreenEvent
import com.zbapp.core.presentation.share.SharingStartScreenState
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.BottomSheetsScreen
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.components.LargeSwitch
import com.zbapp.locationtrack.ui.components.MainAlertDialog
import com.zbapp.locationtrack.ui.groups.GroupListItem
import com.zbapp.locationtrack.ui.groups.SharingViewModel
import com.zbapp.locationtrack.ui.settings.SettingsSectionHeader
import com.zbapp.locationtrack.ui.shared.errorString
import com.zbapp.locationtrack.ui.shared.list.ConditionErrorItem
import com.zbapp.locationtrack.ui.shared.list.nameRes
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharingStartScreen(
    appState: MainAppState,
    groupId: String?,
    vm: SharingViewModel = getViewModel { parametersOf(groupId) },
) {

    val state by vm.uiState.collectAsState(initial = SharingStartScreenState())

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    var batterySwitchState by rememberSaveable { mutableStateOf(false) }
    var locationSwitchState by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        vm.events.collect { event ->
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
                        MainAlertDialog.SharingErrors(state.errors.map { context.getString(it.nameRes) })
                }
                SharingStartScreenEvent.MissingBackgroundPermission -> {
                    showDialog = MainAlertDialog.SharingBackgroundLocationMissing
                }
                SharingStartScreenEvent.SelectGroupInfo -> {
                    appState.showSnackbar(context.getString(R.string.invite_connect_alert_pickGroup))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (val dialog = showDialog) {
        is MainAlertDialog.SharingBackgroundLocationMissing -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    // TODO ask for background location permission
                } else {
                    vm.proceedWithSharing()
                }
            }
        }
        is MainAlertDialog.SharingErrors -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.proceedWithSharing()
                }
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
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    if (state.errors.isNotEmpty()) {
                        Text(text = stringResource(id = R.string.groups_sharing_button_enable))
                    } else {
                        Text(text = stringResource(id = R.string.groups_sharing_button_start))
                    }
                },
                icon = {
                    if (state.errors.isNotEmpty()) {
                        Icon(imageVector = Icons.Rounded.NotListedLocation,
                            contentDescription = null)
                    } else {
                        Icon(imageVector = Icons.Rounded.PersonPinCircle, contentDescription = null)
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp, 3.dp, 3.dp),
                onClick = { vm.startSharing() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(start = 20.dp, end = 20.dp, bottom = 100.dp),
        ) {

            if (state.groupState != null) {
                SettingsSectionHeader(name = stringResource(id = R.string.groups_sharing_section_title_group))
                GroupListItem(
                    modifier = Modifier.fillMaxWidth(),
                    data = state.groupState!!
                ) {
                    appState.uiScope.launch {
                        appState.showBottomSheetForResult(BottomSheetsScreen.GroupPicker(vm.getAllGroups()))
                            .safeCast<GroupItemData>()?.let {
                                vm.setSelectedGroup(it.id)
                            }
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(10.dp))
                FilledButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = ButtonStyle.SMALL,
                    icon = Icons.Rounded.Groups,
                    onClick = {
                        appState.uiScope.launch {
                            appState.showBottomSheetForResult(BottomSheetsScreen.GroupPicker(vm.getAllGroups()))
                                .safeCast<GroupItemData>()?.let {
                                    vm.setSelectedGroup(it.id)
                                }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.invite_connect_button_pickGroup))
                }
                Spacer(modifier = Modifier.size(10.dp))
            }

            SettingsSectionHeader(name = stringResource(id = R.string.groups_sharing_section_title_duration))
            // TODO add time slider
            Spacer(modifier = Modifier.size(50.dp))

            SettingsSectionHeader(name = stringResource(id = R.string.groups_sharing_section_title_extra))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LargeSwitch(
//                    modifier = Modifier.fillMaxWidth(0.45f),
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = stringResource(id = R.string.groups_sharing_setting_battery_share),
                    state = batterySwitchState,
                    onClick = { batterySwitchState = !batterySwitchState }
                )

                Spacer(modifier = Modifier.size(10.dp))
                LargeSwitch(
//                    modifier = Modifier.fillMaxWidth(0.45f),
                    icon = Icons.Rounded.ShareLocation,
                    title = stringResource(id = R.string.groups_sharing_setting_approximate_location),
                    // TODO add slider
                    description = "500m",
                    state = locationSwitchState,
                    onClick = { locationSwitchState = !locationSwitchState }
                )
            }

            Spacer(modifier = Modifier.size(50.dp))

            state.errors.forEach {
                ConditionErrorItem(data = it) {
                    // TODO add handling for errors
//                    when(it) {
//                        ConditionErrorType.GPS -> TODO()
//                        ConditionErrorType.NETWORK -> TODO()
//                        ConditionErrorType.PERMISSION_LOCATION -> TODO()
//                        ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> TODO()
//                        ConditionErrorType.PERMISSION_LOCATION_PRECISE -> TODO()
//                    }
                }
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