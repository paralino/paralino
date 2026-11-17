package app.paralino.android.ui.settings.profile

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.resources.moko.*
import app.paralino.core.presentation.settings.userdevices.UserDevicesViewModel
import app.paralino.core.ui.components.DefaultToolbar
import app.paralino.core.ui.components.ToolbarButtons
import app.paralino.core.ui.settings.profile.UserDevicesScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun UserDevicesScreen(
    appState: MainAppState,
    vm: UserDevicesViewModel = koinViewModel(),
) {
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_devices_screen_title),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) { padding ->
        UserDevicesScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = padding,
        )
    }
}
