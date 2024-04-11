package app.paralino.android.ui.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.ui.components.dialogs.Show
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.components.dialogs.MainAlertDialog
import app.paralino.core.ui.settings.profile.AccountScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun AccountScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {
    var showDialog by remember { mutableStateOf<MainAlertDialog?>(null) }
    val context = LocalContext.current

    AccountScreenCore(
        appState = appState,
        viewModel = viewModel,
        onShowDialog = { showDialog = it },
        openProfileRawScreen = {
            appState.navController.navigate(
                AppScreen.Settings.ProfileDetails.RawDataScreen.route(
                    RawPresentationDataType.UserPresentation
                )
            )
        },
        openConvertAccount = {
            appState.navController.navigate(
                AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature
            )
        },
        openEditProfile = {
            appState.navController.navigate(AppScreen.Settings.ProfileDetails.EditProfile.routeSignature)
        },
        openChangeEmail = {
            appState.navController.navigate(AppScreen.Settings.ProfileDetails.ChangeEmail.routeSignature)
        },
        openChangePassword = {
            appState.navController.navigate(AppScreen.Settings.ProfileDetails.ChangePassword.routeSignature)
        },
        onShare = {
            context.share(it)
        }
    )


    when (val dialog = showDialog) {
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

        is MainAlertDialog.VerifyEmail -> {
            dialog.Show {
                showDialog = null
                if (it == MainAlertDialog.Buttons.buttonOk) {
                    viewModel.sendEmailVerification()
                }
            }
        }

        else -> {}
    }
}