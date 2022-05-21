package com.zbapp.locationtrack.ui.share

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zbapp.core.presentation.dashboard.GroupItemData
import com.zbapp.core.presentation.share.SharingStartScreenState
import com.zbapp.core.presentation.share.SharingStartViewModel
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.BottomSheetsScreen
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.components.LargeSwitch
import com.zbapp.locationtrack.ui.components.LargeSwitchDefaults
import com.zbapp.locationtrack.ui.groups.GroupListItem
import com.zbapp.locationtrack.ui.groups.SharingViewModel
import com.zbapp.locationtrack.ui.settings.SettingsSectionHeader
import com.zbapp.locationtrack.ui.shared.list.ConditionErrorItem
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.safeCast
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharingStartScreen(
    appState: MainAppState,
    groupId: String?,
    vm: SharingStartViewModel
) {

    val state by vm.uiState.collectAsState(initial = SharingStartScreenState())
    var batterySwitchState by rememberSaveable { mutableStateOf(false) }
    var locationSwitchState by rememberSaveable { mutableStateOf(false) }

    Scaffold(
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
        }
    ) {
        Column {

            if(state.groupState != null) {
                SettingsSectionHeader(name = stringResource(id = R.string.groups_sharing_section_title_group))
                GroupListItem(data = state.groupState!!) {
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

            LazyVerticalGrid(
                cells = GridCells.Adaptive(minSize = LargeSwitchDefaults.minWidth),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                this.item {
                    LargeSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = stringResource(id = R.string.groups_sharing_setting_battery_share),
                        state = batterySwitchState,
                        onClick = { batterySwitchState = !batterySwitchState }
                    )
                }

                this.item {
                    LargeSwitch(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.ShareLocation,
                        title = stringResource(id = R.string.groups_sharing_setting_approximate_location),
                        // TODO add slider
                        description = "500m",
                        state = locationSwitchState,
                        onClick = { locationSwitchState = !locationSwitchState }
                    )
                }
            }

            Spacer(modifier = Modifier.size(100.dp))

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
}