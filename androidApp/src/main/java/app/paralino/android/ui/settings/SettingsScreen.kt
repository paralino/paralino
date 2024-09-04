package app.paralino.android.ui.settings

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.MR
import app.paralino.core.presentation.rawdata.RawPresentationDataType
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.SettingSectionInfo
import app.paralino.core.ui.settings.SettingsScreenCore
import app.paralino.core.ui.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = getViewModel(),
) {

    val context = LocalContext.current
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.title_settings),
                navigationIcon = { ToolbarButtons.Close { appState.navController.popBackStack() } }
            )
        }
    ) {
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
            openProfileRawScreen = {
                appState.navController.navigate(
                    AppScreen.Settings.ProfileDetails.RawDataScreen.route(
                        RawPresentationDataType.UserPresentation
                    )
                )
            },
            openLicences = {
                appState.navController.navigate(AppScreen.Settings.Licences.routeSignature)
            },
            openAppStatus = {
                appState.navController.navigate(AppScreen.Settings.AppStatusDebug.routeSignature)
            },
            openSubscriptions = {
                appState.navController.navigate(AppScreen.Settings.Subscriptions.routeSignature)
            },
            onShareCode = {
                context.share(it)
            }
        )
    }
}

@Preview(widthDp = 320, heightDp = 600)
@Preview(widthDp = 320, heightDp = 500, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainPreview() {
    AppTheme {
        Surface {
            SettingSectionInfo(
                appVersion = "v1.2.3 (43)",
                isErrorReportingEnabled = true,
                onLicencesClick = {},
                onAppStatusClick = {},
                onShareApp = {},
                toggleErrorReporting = {},
            )
        }
    }
}