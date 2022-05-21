package com.zbapp.locationtrack.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.Payload
import com.zbapp.core.utils.ConditionErrorType
import com.zbapp.locationtrack.AppClass
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.tracking.LocationPermissionHandler
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.DefaultEmptyState
import com.zbapp.locationtrack.ui.components.FilledButton
import com.zbapp.locationtrack.ui.groups.GroupListItem
import com.zbapp.locationtrack.ui.navigation.AppScreen
import com.zbapp.locationtrack.ui.settings.SettingsSectionHeader
import com.zbapp.locationtrack.ui.shared.list.ConditionErrorItem
import com.zbapp.locationtrack.ui.theme.ButtonStyle
import com.zbapp.locationtrack.utils.extensions.getActivity
import org.koin.androidx.compose.getViewModel

@Composable
fun DashboardScreen(
    appState: MainAppState,
    vm: DashboardViewModel = getViewModel(),
) {

    val activity = LocalContext.current.getActivity()!!
    val permissionHandler = LocationPermissionHandler(
        activity.application as AppClass,
        activity,
        activity.activityResultRegistry
    )
    val vmState by vm.uiState.collectAsState(Payload.Loading)
    val state = vmState

    when (state) {
        is Payload.Error -> {
            DefaultEmptyState(content = stringResource(id = R.string.error_something_wrong))
        }
        Payload.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        is Payload.Success -> {
            val scrollState = rememberScrollState()
            // TODO replace with lazy column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {

                state.data.errors.forEach {
                    ConditionErrorItem(data = it) {
                        when (it) {
                            ConditionErrorType.GPS -> permissionHandler.askLocationService()
                            ConditionErrorType.PERMISSION_LOCATION -> permissionHandler.askLocationPermission()
                            ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> permissionHandler.askBackgroundLocationPermission()
                        }
                    }
                }

                if (state.data.isWithoutGroups) {
                    DefaultEmptyState(
                        content = stringResource(id = R.string.dashboard_info_no_groups),
                        icon = Icons.Rounded.Groups,
                        buttonText = stringResource(id = R.string.dashboard_groups_add_button),
                        bigButton = true,
                        onClick = {
                            appState.navController.navigate(AppScreen.DashboardRoot.GroupCreate.route())
                        }
                    )
                } else {

                    if (state.data.activeGroups.isNotEmpty()) {
                        SettingsSectionHeader(name = stringResource(id = R.string.dashboard_groups_section_active))
                        state.data.activeGroups.forEach {
                            GroupListItem(data = it) {
                                appState.navController.navigate(AppScreen.DashboardRoot.GroupDetails.route(it.id))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }

                    if (state.data.inactiveGroups.isNotEmpty()) {
                        SettingsSectionHeader(name = stringResource(id = R.string.dashboard_groups_section_inactive))
                        state.data.inactiveGroups.forEach {
                            GroupListItem(data = it) {
                                appState.navController.navigate(AppScreen.DashboardRoot.GroupDetails.route(it.id))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.size(100.dp))
                    FilledButton(
                        style = ButtonStyle.SMALL,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        icon = Icons.Rounded.GroupAdd,
                        onClick = {
                            appState.navController.navigate(AppScreen.DashboardRoot.GroupCreate.route())
                        }
                    ) {
                        Text(text = stringResource(id = R.string.dashboard_groups_add_button))
                    }

                }
            }
        }
    }

}
