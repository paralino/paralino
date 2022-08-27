package app.paralino.android.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.paralino.core.models.Payload
import app.paralino.core.models.base.ImageModel
import app.paralino.core.presentation.settings.AccountScreenEvent
import app.paralino.core.utils.Emojis
import app.paralino.android.R
import app.paralino.android.ui.MainAppState
import app.paralino.android.ui.components.ImageModelImage
import app.paralino.android.ui.components.MainAlertDialog
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.ui.shared.errorString
import app.paralino.android.utils.extensions.annotatedBold
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
        SettingsBigHeader(name = stringResource(id = R.string.settings_section_account_title))

        when (val state = accountState) {
            is Payload.Success -> {
                AccountInfoHeader(
                    name = state.data.userState.name,
                    email = state.data.userState.email,
                    userId = state.data.userState.userId,
                    image = state.data.userState.image ?: ImageModel.emoji(Emojis.robot)
                )
            }
            Payload.Loading -> {}
            is Payload.Error -> {}
        }

        SettingsActionItem(
            name = stringResource(id = R.string.settings_account_edit_profile),
            icon = Icons.Rounded.Person
        ) {
            appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.EditProfile.routeSignature)
        }
        SettingsActionItem(
            name = stringResource(id = R.string.settings_account_edit_email),
            icon = Icons.Rounded.Email
        ) {
            appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.ChangeEmail.routeSignature)
        }
        SettingsActionItem(
            name = stringResource(id = R.string.settings_account_edit_password),
            icon = Icons.Rounded.Password
        ) {
            appState.navController.navigate(AppScreen.SettingsRoot.ProfileDetails.ChangePassword.routeSignature)
        }

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

@Composable
fun AccountInfoHeader(
    modifier: Modifier = Modifier,
    name: String,
    email: String,
    userId: String,
    image: ImageModel,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        Spacer(modifier = Modifier.size(20.dp))

        ImageModelImage(
            modifier = Modifier.size(100.dp),
            imageModel = image
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold)
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
            Text(
                text = "Email: **$email**".annotatedBold(),
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
            )
            Text(
                text = "ID: **$userId**".annotatedBold(),
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Light)
            )
        }

        Spacer(modifier = Modifier.size(20.dp))
    }
}