package app.paralino.android.ui.settings.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.MR
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.profile.AccountScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AccountScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_section_account_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        AccountScreenCore(
            appState = appState,
            viewModel = viewModel,
            openConvertAccount = {
                appState.navController.navigate(
                    AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature
                )
            },
            openProfileRawScreen = {
                appState.navController.navigate(
                    AppScreen.Settings.ProfileDetails.RawDataScreen.route(
                        RawPresentationDataType.UserPresentation
                    )
                )
            },
            openEditProfile = {
                appState.navController.navigate(AppScreen.Settings.ProfileDetails.EditProfile.routeSignature)
            },
            openBlockList = {
                appState.navController.navigate(AppScreen.Settings.BlockList.routeSignature)
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
}