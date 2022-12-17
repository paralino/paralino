package app.paralino.android.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.paralino.android.R
import app.paralino.android.notification.rememberNotificationsPermissionHandler
import app.paralino.core.models.Payload
import app.paralino.core.presentation.dashboard.DashboardScreenEvent
import app.paralino.core.utils.ConditionErrorType
import app.paralino.android.tracking.rememberLocationPermissionHandler
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.ConditionErrorItem
import app.paralino.android.ui.components.DefaultEmptyState
import app.paralino.android.ui.components.FilledButton
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.groups.GroupListItem
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.settings.SettingsBigHeader
import app.paralino.android.ui.settings.SettingsSectionHeader
import app.paralino.android.ui.shared.errorString
import app.paralino.android.ui.theme.ButtonStyle
import app.paralino.core.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel


@Composable
fun DashboardScreen(
    appState: MainAppState,
    vm: DashboardViewModel = getViewModel(),
) {

    val locationPermissionHandler = rememberLocationPermissionHandler(
        appState = appState,
        locationStatusProvider = vm.locationStatusProvider
    )

    val notificationPermissionHandler = rememberNotificationsPermissionHandler(
        appState = appState,
        locationStatusProvider = vm.locationStatusProvider
    )

    val vmState by vm.uiState.collectAsState(Payload.Loading)
    val state = vmState

    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }

    val context = LocalContext.current

    LaunchedEffect(key1 = context) {
        vm.refreshGroups()
    }

    LaunchedEffect(key1 = context) {
        vm.events.collectLatest { event ->
            when (event) {
                is DashboardScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                DashboardScreenEvent.EmailVerificationSent -> {
                    appState.showSnackbar(context.getString(R.string.info_email_check_instructions))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when(val dialog = showDialog) {
        is MainAlertDialog.VerifyEmail -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    vm.sendEmailVerification()
                }
            }
        }
        else -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {

        SettingsBigHeader(name = stringResource(id = R.string.title_dashboard))

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
                ) {

                    state.data.errors.forEach {
                        ConditionErrorItem(data = it) {
                            when (it) {
                                ConditionErrorType.GPS -> locationPermissionHandler.askLocationService()
                                ConditionErrorType.PERMISSION_NOTIFICATIONS -> notificationPermissionHandler.askNotificationPermission()
                                ConditionErrorType.PERMISSION_LOCATION -> locationPermissionHandler.askLocationPermission()
                                ConditionErrorType.PERMISSION_LOCATION_BACKGROUND -> locationPermissionHandler.askBackgroundLocationPermission()
                                ConditionErrorType.EMAIL_VERIFICATION -> showDialog = MainAlertDialog.VerifyEmail(state.data.email)
                                else -> { /* TODO review or do nothing */ }
                            }
                        }
                    }

                    if (state.data.isWithoutGroups) {
                        DefaultEmptyState(
                            content = stringResource(id = R.string.dashboard_info_no_groups),
                            icon = Icons.Rounded.Groups,
                            buttons = {
                                DashboardGroupJoinButtons(appState)
                            }
                        )
                    } else {

                        if (state.data.activeGroups.isNotEmpty()) {
                            SettingsSectionHeader(name = stringResource(id = R.string.dashboard_groups_section_active))
                            state.data.activeGroups.forEach {
                                GroupListItem(data = it) {
                                    appState.navController.navigate(
                                        AppScreen.DashboardRoot.GroupDetails.route(it.id)
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }

                        if (state.data.inactiveGroups.isNotEmpty()) {
                            SettingsSectionHeader(name = stringResource(id = R.string.dashboard_groups_section_inactive))
                            state.data.inactiveGroups.forEach {
                                GroupListItem(data = it) {
                                    appState.navController.navigate(
                                        AppScreen.DashboardRoot.GroupDetails.route(it.id)
                                    )
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.size(100.dp))

                        DashboardGroupJoinButtons(appState)
                    }
                }
            }
        }
    }

}

@Composable
fun ColumnScope.DashboardGroupJoinButtons(appState: MainAppState) {
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

    Spacer(modifier = Modifier.size(10.dp))

    FilledButton(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        style = ButtonStyle.SMALL,
        icon = Icons.Rounded.Key,
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        onClick = {
            appState.navController.navigate(AppScreen.SettingsRoot.PersonalCodes.route(createCode = true))
        }
    ) {
        Text(text = stringResource(id = R.string.dashboard_groups_join_button))
    }
}
