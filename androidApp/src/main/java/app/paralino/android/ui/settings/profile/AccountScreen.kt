package app.paralino.android.ui.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.settings.profile.AccountScreenCore
import org.koin.androidx.compose.getViewModel

@Composable
fun AccountScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {
    val context = LocalContext.current

    AccountScreenCore(
        appState = appState,
        viewModel = viewModel,
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
}