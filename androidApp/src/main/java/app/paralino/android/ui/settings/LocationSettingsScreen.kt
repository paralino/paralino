package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.settings.location.LocationSettingsViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.location.LocationSettingsScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@Composable
fun LocationSettingsScreen(
    appState: MainAppState,
    vm: LocationSettingsViewModel = getViewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.setting_location_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        },
    ) {
        LocationSettingsScreenCore(
            coreUiState = appState,
            vm = vm,
            paddingValues = it,
            openPaywall = {
                appState.navController.navigate(AppScreen.Settings.BillingPaywall.route())
            }
        )
    }
}