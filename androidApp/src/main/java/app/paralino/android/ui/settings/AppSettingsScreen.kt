package app.paralino.android.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.settings.generic.AppSettingsViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.AppSettingsScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppSettingsScreen(
    appState: MainAppState,
    vm: AppSettingsViewModel = koinViewModel()
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
            vm = vm,
            appState = appState,
            paddingValues = it,
            onMeasurementSystemClick = {
                appState.navController.navigate(AppScreen.Settings.MeasurementSystem.routeSignature)
            },
            onNotificationGroupingClick = {
                appState.navController.navigate(AppScreen.Settings.NotificationGrouping.routeSignature)
            },
            onMapInteractionsClick = {
                appState.navController.navigate(AppScreen.Settings.MapInteractions.routeSignature)
            },
            onNavigationAppClick = {},
            onPlaceSearchSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.PlaceSearch.routeSignature)
            },
            onPersonalSpaceSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.PersonalSpace.routeSignature)
            },
            onNotificationSocketSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.NotificationSocket.routeSignature)
            },
            onMapSourceSettingsClick = {
                appState.navController.navigate(AppScreen.Settings.MapsProvider.routeSignature)
            }
        )
    }
}