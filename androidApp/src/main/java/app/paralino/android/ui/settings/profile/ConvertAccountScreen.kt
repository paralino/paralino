package app.paralino.android.ui.settings.profile

import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import app.paralino.android.ui.main.MainAppState
import app.paralino.core.MR
import app.paralino.core.presentation.settings.convertaccount.ConvertAccountViewModel
import app.paralino.core.ui.components.dialogs.DefaultToolbar
import app.paralino.core.ui.components.dialogs.ToolbarButtons
import app.paralino.core.ui.settings.profile.ConvertAccountScreenCore
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.getViewModel

@Composable
fun ConvertAccountScreen(
    appState: MainAppState,
    vm: ConvertAccountViewModel = getViewModel(),
) {
    Scaffold(
        topBar = {
            DefaultToolbar(
                title = stringResource(resource = MR.strings.settings_account_guest_convert),
                navigationIcon = { ToolbarButtons.Back { appState.navController.popBackStack() } }
            )
        }
    ) {
        ConvertAccountScreenCore(
            appState = appState,
            vm = vm,
            contentPadding = it,
            onNavigateBack = { appState.navController.popBackStack() }
        )
    }
}