package app.paralino.android.ui.groups

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.android.ui.navigation.AppScreen
import app.paralino.core.MR
import app.paralino.core.presentation.groups.settings.BatteryNotificationsViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.groups.BatteryNotificationsScreenCore
import app.paralino.core.ui.settings.billing.BillingScreenSourceType
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BatteryNotificationsScreen(
    appState: MainAppState,
    groupId: String,
    vm: BatteryNotificationsViewModel = koinViewModel { parametersOf(groupId) },
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(MR.strings.group_settings_battery_notifications_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        BatteryNotificationsScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = it,
            openPaywall = {
                appState.navController.navigate(
                    AppScreen.MainHome.BillingPaywall.route(
                        groupId = groupId,
                        type = BillingScreenSourceType.UPGRADE
                    )
                )
            }
        )
    }
}