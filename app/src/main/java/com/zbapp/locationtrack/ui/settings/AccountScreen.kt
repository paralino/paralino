package com.zbapp.locationtrack.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zbapp.core.models.Payload
import com.zbapp.core.presentation.settings.AccountScreenEvent
import com.zbapp.locationtrack.R
import com.zbapp.locationtrack.ui.MainAppState
import com.zbapp.locationtrack.ui.components.MainAlertDialog
import com.zbapp.locationtrack.ui.navigation.AppScreen
import com.zbapp.locationtrack.ui.shared.errorString
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.getViewModel

@Composable
fun AccountScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {
    val scrollState = rememberScrollState()
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    var showDangerZoneContent by remember { mutableStateOf(false) }
    val accountState by viewModel.userData.collectAsState(Payload.Loading)

    val context = LocalContext.current
    LaunchedEffect(key1 = context) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AccountScreenEvent.Error -> {
                    appState.showSnackbar(event.error.errorString(context))
                }
                null -> {
                    /* do nothing */
                }
            }
        }
    }

    when (showDialog) {
        is MainAlertDialog.Logout -> {
            MainAlertDialog.Logout.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    viewModel.logout()
                }
            }
        }
        is MainAlertDialog.DeleteAccount -> {
            MainAlertDialog.DeleteAccount.Show {
                showDialog = when (it) {
                    MainAlertDialog.Buttons.buttonOk -> MainAlertDialog.DeleteAccountConfirm
                    else -> null
                }
            }
        }
        is MainAlertDialog.DeleteAccountConfirm -> {
            MainAlertDialog.DeleteAccountConfirm.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    viewModel.deleteAccount()
                }
            }
        }
        else -> {}
    }

    Column(
        modifier = Modifier
            .verticalScroll(state = scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        SettingsSectionHeader(name = stringResource(id = R.string.settings_section_account_title))

        // TODO add proper account header
        when (val state = accountState) {
            is Payload.Success -> {
                Text(text = "Name: ${state.data.userState.name}")
                Text(text = "Email: ${state.data.userState.email}")
                Text(text = "UserId: ${state.data.userState.userId}")
            }
            Payload.Loading -> {}
            is Payload.Error -> {}
        }

        SettingsActionItem(
            name = stringResource(id = R.string.settings_account_edit_profile),
            icon = Icons.Rounded.Person
        ) { }
        SettingsActionItem(
            name = stringResource(id = R.string.settings_account_edit_email),
            icon = Icons.Rounded.Email
        ) {}
        SettingsActionItem(
            name = stringResource(id = R.string.settings_account_edit_password),
            icon = Icons.Rounded.Password
        ) {}

        SettingsActionItem(
            name = stringResource(id = R.string.setting_title_logout),
            icon = Icons.Rounded.Logout
        ) {
            showDialog = MainAlertDialog.Logout
        }

        AnimatedVisibility(visible = showDangerZoneContent) {
            Column {
                SettingsSectionHeader(name = stringResource(id = R.string.setting_title_dangerZone))
                SettingsActionItem(
                    name = stringResource(id = R.string.setting_title_delete_account),
                    icon = Icons.Rounded.DeleteForever
                ) {
                    showDialog = MainAlertDialog.DeleteAccount
                }
            }
        }

        AnimatedVisibility(visible = !showDangerZoneContent) {
            SettingsActionItem(
                name = stringResource(id = R.string.setting_title_dangerZone),
                icon = Icons.Rounded.Warning
            ) {
                showDangerZoneContent = true
            }
        }
    }
}