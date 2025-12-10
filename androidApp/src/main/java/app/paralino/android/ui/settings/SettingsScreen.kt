package app.paralino.android.ui.settings

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.share
import app.paralino.core.MR
import app.paralino.core.presentation.settings.SettingsViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.SettingsScreenCore
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import app.paralino.core.ui.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    appState: MainAppState,
    viewModel: SettingsViewModel = koinViewModel(),
) {

    val context = LocalContext.current
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .safeDrawingPadding(),
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
            openGeneralInfo = {
                appState.navController.navigate(AppScreen.Settings.GeneralInfo.routeSignature)
            },
            openAppStatus = {
                appState.navController.navigate(AppScreen.Settings.AppStatusDebug.routeSignature)
            },
            openSubscriptions = {
                appState.navController.navigate(AppScreen.Settings.Subscriptions.route())
            },
            openPaywall = {
                appState.navController.navigate(AppScreen.Settings.BillingPaywall.route(type = BillingScreenSourceType.NORMAL))
            },
            openAppSettings = {
                appState.navController.navigate(AppScreen.Settings.AppSettings.routeSignature)
            },
            openLocationSettings = {
                appState.navController.navigate(AppScreen.Settings.LocationSettings.routeSignature)
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
        }
    }
}