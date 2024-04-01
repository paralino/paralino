package app.paralino.android.ui.settings

import android.content.res.Configuration
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.settings.SettingSectionInfo
import app.paralino.core.ui.settings.SettingsScreenCore
import app.paralino.core.ui.theme.AppTheme
import org.koin.androidx.compose.getViewModel

@Composable
fun SettingsScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {

    val context = LocalContext.current
    SettingsScreenCore(
        appState = appState,
        viewModel = viewModel,
        onShareApp = { context.share(it) },
        openConvertAccount = {
            appState.navController.navigate(
                AppScreen.Settings.ProfileDetails.ConvertAccount.routeSignature
            )
        },
        openProfileDetails = {
            appState.navController.navigate(AppScreen.Settings.ProfileDetails.routeSignature)
        },
        openPersonalCodes = {
            appState.navController.navigate(AppScreen.Settings.PersonalCodes.routeSignature)
        },
        openBlockList = {
            appState.navController.navigate(AppScreen.Settings.BlockList.routeSignature)
        },
        openLicences = {
            appState.navController.navigate(AppScreen.Settings.Licences.routeSignature)
        }
    )
}

@Preview(widthDp = 320, heightDp = 600)
@Preview(widthDp = 320, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainPreview() {
    AppTheme {
        Surface {
            SettingSectionInfo(
                appVersion = "v1.2.3 (43)",
                onLicencesClick = {},
                onShareApp = {}
            )
        }
    }
}