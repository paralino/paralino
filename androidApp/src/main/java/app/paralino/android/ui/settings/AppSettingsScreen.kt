package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.android.utils.extensions.contactSupport
import app.paralino.core.MR
import app.paralino.core.presentation.settings.general.GeneralInfoViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.AppSettingsScreenCore
import app.paralino.core.ui.settings.GeneralInfoScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppSettingsScreen(
    appState: MainAppState
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding().fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.title_settings),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        AppSettingsScreenCore(
            appState = appState,
            paddingValues = it,
            onMeasurementSystemClick = {
                appState.navController.navigate(AppScreen.Settings.MeasurementSystem.routeSignature)
            },
            onNotificationGroupingClick = {
                appState.navController.navigate(AppScreen.Settings.NotificationGrouping.routeSignature)
            }
        )
    }
}